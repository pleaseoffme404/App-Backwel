package com.backwell.auth_server.resolver;

import com.backwell.auth_server.security.user.IdentityContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolver personalizado de Spring MVC encargado de inyectar automáticamente el DTO del usuario autenticado
 * en los parámetros de los métodos de los controladores que utilicen la anotación {@link CurrentUser}.
 * <p>
 * Extrae la información directamente del contexto de seguridad de Spring Security
 * evadiendo la necesidad de acceder manualmente a {@link SecurityContextHolder} en cada endpoint.
 * </p>
 *
 * @author J-Sinclaire
 * @see HandlerMethodArgumentResolver
 * @see CurrentUser
 */
@Component
@Slf4j
public class UserDTOArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Evalúa si el parámetro del controlador cumple con los requisitos para ser resuelto por esta clase.
     * <p>
     * Se requiere que el parámetro esté anotado con {@link CurrentUser} y que su tipo de dato coincida
     * con la clase esperada (ej. {@code UserDTO} o la clase que retorna tu contenedor de identidad).
     * </p>
     *
     * @param parameter el parámetro del método del controlador a evaluar.
     * @return {@code true} si el parámetro tiene la anotación {@code @CurrentUser} y coincide con el tipo esperado;
     * {@code false} en caso contrario.
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // NOTA: Cambié IdentityContainer.class por el tipo real que devuelve tu método getUserDTO().
        // Si tu método devuelve un tipo 'UserDTO', aquí debe ir 'UserDTO.class' para evitar ClassCastException.
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().isAssignableFrom(IdentityContainer.class);
    }

    /**
     * Resuelve el argumento del controlador extrayendo el principal desde el contexto de Spring Security.
     *
     * @param parameter el parámetro del método del controlador que se va a resolver.
     * @param mavContainer el contenedor de modelos y vistas actual (puede ser nulo).
     * @param webRequest la solicitud web nativa actual.
     * @param binderFactory la fábrica para crear el vinculador de datos (puede ser nulo).
     * @return El objeto DTO del usuario autenticado, o {@code null} si no hay una sesión activa o el principal
     * no coincide con la instancia de {@link IdentityContainer} esperada.
     */
    @NonNull
    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    "No valid authentication was found in the security context to resolve the parameter: '%s'"
                            .formatted(parameter.getParameterName())
            );
        }

        if (authentication.getPrincipal() instanceof IdentityContainer identityContainer) {
            log.debug("Resolved identity container for user '{}'", parameter.getParameterName());
            return identityContainer.getUserDTO();
        }

        throw new IllegalStateException(
                "Authentication Principal does not match IdentityContainer class. (Type found: [%s])"
                        .formatted(parameter.getParameterName())
        );
    }
}