package com.backwell.api_service.common.config.user;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;


@AllArgsConstructor
@Slf4j
public class UserSessionResolver implements HandlerMethodArgumentResolver {
    private final UserSessionProvider userSessionProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserSession.class);
    }

    @Nullable
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) throws Exception {
        try {
            return userSessionProvider.getCurrentUserSession();
        } catch (Exception e) {
            log.error("Fatal error while resolving user session.");
            log.error(e.getMessage(), e);
            throw e;
        }
    }
}
