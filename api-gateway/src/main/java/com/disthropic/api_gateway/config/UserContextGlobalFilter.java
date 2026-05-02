package com.disthropic.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class UserContextGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication != null && authentication.isAuthenticated())
                .flatMap(authentication -> {
                    ServerHttpRequest mutatedRequest = addUserHeaders(exchange.getRequest(), authentication);

                    log.debug("User context added for: {}", authentication.getName());

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private ServerHttpRequest addUserHeaders(ServerHttpRequest request, Authentication authentication) {
        ServerHttpRequest.Builder builder = request.mutate();

        builder.header("X-Authenticated-User", authentication.getName());

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            Object principal = oauthToken.getPrincipal();

            if (principal instanceof OidcUser oidcUser) {
                builder.header("X-User-Email", Optional.ofNullable(oidcUser.getEmail()).orElse(""));
                builder.header("X-User-Name", Optional.ofNullable(oidcUser.getFullName()).orElse(""));
            } else if (principal != null) {
                OAuth2User oauth2User = (OAuth2User) principal;
                builder.header("X-User-Name", Objects.requireNonNull(oauth2User.getAttribute("name")));
                builder.header("X-User-Email", Objects.requireNonNull(oauth2User.getAttribute("email")));
            }

            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            attributes.forEach((key, value) -> {
                if (value != null && !key.equals("sub") && !key.equals("email")) {
                    builder.header("X-User-" + key, value.toString());
                }
            });
        }

        String traceId = Optional.ofNullable(request.getHeaders().getFirst("X-Trace-Id"))
                .orElse(java.util.UUID.randomUUID().toString());
        builder.header("X-Trace-Id", traceId);

        return builder.build();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}