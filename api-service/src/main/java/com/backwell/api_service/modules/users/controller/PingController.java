package com.backwell.api_service.modules.users.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ping")
@RequiredArgsConstructor
public class PingController {

    /**
     * 1. PING BÁSICO
     * Verifica que el microservicio responde y el enrutamiento del Gateway funciona.
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Pong!",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * 2. DEBUG DE IDENTIDAD (JWT)
     * Verifica que el 'TokenRelay' del Gateway inyectó el JWT y Spring Security lo validó.
     * Si este endpoint devuelve 401, el token no llegó o el 'issuer' es incorrecto.
     */
    @GetMapping("/whoami")
    public ResponseEntity<Map<String, Object>> whoAmI(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No se encontró un token JWT válido."));
        }

        return ResponseEntity.ok(Map.of(
                "subject", jwt.getSubject(),               // El UUID del usuario (sub)
                "issuer", jwt.getIssuer().toString(),      // Quien emitió el token (iss)
                "claims", jwt.getClaims(),                 // Todos los datos del usuario
                "authorities", jwt.getClaimAsStringList("scope") // Scopes (openid, profile, etc)
        ));
    }

    /**
     * 3. DEBUG DE CABECERAS
     * Verifica físicamente qué le está mandando el Gateway al API Service.
     * Útil para ver si llega el header 'Authorization: Bearer ...'
     */
    @GetMapping("/headers")
    public ResponseEntity<Map<String, String>> debugHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames())
                .forEach(h -> headers.put(h, request.getHeader(h)));

        // Maskeamos el token por seguridad en los logs
        if (headers.containsKey("authorization")) {
            headers.computeIfPresent("authorization", (k, auth) -> auth.substring(0, 15) + "...");
        }

        return ResponseEntity.ok(headers);
    }
}
