package com.backwell.auth_server.config;

import com.backwell.auth_server.config.properties.ClientRegistrationProperties;
import com.backwell.auth_server.config.properties.JwtProperties;
import com.backwell.auth_server.security.mixin.AppOidcUserMixin;
import com.backwell.auth_server.security.mixin.AppUserDetailsMixin;
import com.backwell.auth_server.security.mixin.UserDTOMixin;
import com.backwell.auth_server.security.service.UserSecurityService;
import com.backwell.auth_server.security.user.AppOidcUser;
import com.backwell.auth_server.security.user.AppUserDetails;
import com.backwell.auth_server.security.user.UserDTO;
import com.backwell.auth_server.service.UUIDGeneratorService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationServerConfig {
    private final UUIDGeneratorService uuidService;
    private final PasswordEncoder passwordEncoder;
    private final ClientRegistrationProperties clientProperties;
    private final UserSecurityService userSecurityService;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final FederatedIdentitySuccessHandler federatedIdentitySuccessHandler;

    private String[] whiteList() {
        return new String[]{
                "/login", "/error", "/oauth2/**", "/.well-known/**",
                "/actuator/**", "/register", "/css/**", "/js/**", "/webjars/**",
                "/register/**"
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http
                .securityMatcher(authServerConfigurer.getEndpointsMatcher())
                .with(authServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .oidc(Customizer.withDefaults())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2-> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .exceptionHandling(ex-> ex
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .cors(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/login","/logout", "/oauth2/**", "/register/**")
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(whiteList()).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(userSecurityService))
                        .successHandler(federatedIdentitySuccessHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new  LoginUrlAuthenticationEntryPoint("/login"))
                        .accessDeniedHandler(accessDeniedHandler))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                );

        return http.build();
    }


    // ********** Client Registration Bean
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        List<RegisteredClient> registrations = clientProperties.buildAll(
                passwordEncoder,
                uuidService
        );
        return new InMemoryRegisteredClientRepository(registrations);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JwtProperties jwtProperties) {
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(jwtProperties.getPrivateKey());
            byte[] publicKeyBytes = Base64.getDecoder().decode(jwtProperties.getPublicKey());

            KeyFactory kf = KeyFactory.getInstance("RSA");

            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                    .privateKey((RSAPrivateKey) privateKey)
                    .keyID(jwtProperties.getKeyId())
                    .build();
            return new ImmutableJWKSet<>(new JWKSet(rsaKey));
        } catch (Exception e) {
            throw new RuntimeException("Unable to load cryptographic keys ");
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(JwtProperties jwtProperties) {
        return AuthorizationServerSettings.builder()
                .issuer(jwtProperties.getIssuer())
                .build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        JdbcOAuth2AuthorizationService service = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper rowMapper = new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(registeredClientRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();

        // 1. Módulos base de Spring Security y OAuth2
        objectMapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 3. Tus Mixins actualizados para Records
        objectMapper.addMixIn(UserDTO.class, UserDTOMixin.class);
        objectMapper.addMixIn(AppUserDetails.class, AppUserDetailsMixin.class);
        objectMapper.addMixIn(AppOidcUser.class, AppOidcUserMixin.class);

        rowMapper.setObjectMapper(objectMapper);
        service.setAuthorizationRowMapper(rowMapper);

        return service;
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }
}
