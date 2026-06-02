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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserContextGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication != null && authentication.isAuthenticated())
                .flatMap(authentication -> {
                    ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

                    builder.header("X-Authenticated-User", authentication.getName());

                    if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                        Object principal = oauthToken.getPrincipal();

                        if (principal instanceof OidcUser oidcUser) {
                            builder.header("X-User-Email", Optional.ofNullable(oidcUser.getEmail()).orElse(""));
                            builder.header("X-User-Name", Optional.ofNullable(oidcUser.getFullName()).orElse(""));
                        } else if (principal instanceof OAuth2User oauth2User) {

                            String name = Optional.ofNullable(oauth2User.getAttribute("name")).map(Object::toString).orElse("");
                            String email = Optional.ofNullable(oauth2User.getAttribute("email")).map(Object::toString).orElse("");
                            builder.header("X-User-Name", name);
                            builder.header("X-User-Email", email);
                        }

                        Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
                        attributes.forEach((key, value) -> {
                            if (value != null && !key.equals("sub") && !key.equals("email") && !key.equals("name")) {
                                if (value instanceof List<?> list) {

                                    String flattenedList = list.stream()
                                            .map(Object::toString)
                                            .collect(Collectors.joining(","));
                                    builder.header("X-User-" + key, flattenedList);
                                } else {
                                    builder.header("X-User-" + key, value.toString());
                                }
                            }
                        });
                    }
                    
                    String traceId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
                            .orElseGet(() -> UUID.randomUUID().toString());
                    builder.header("X-Trace-Id", traceId);

                    log.debug("User context and Trace ID [{}] added for: {}", traceId, authentication.getName());

                    return chain.filter(exchange.mutate().request(builder.build()).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    String traceId = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
                            .orElseGet(() -> UUID.randomUUID().toString());

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-Trace-Id", traceId)
                            .build();

                    log.trace("Anonymous request - Trace ID [{}] generated", traceId);

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }));
    }

    @Override
    public int getOrder() {
        
        return Ordered.HIGHEST_PRECEDENCE;
    }
}