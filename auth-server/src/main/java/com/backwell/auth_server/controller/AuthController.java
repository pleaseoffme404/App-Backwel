package com.backwell.auth_server.controller;

import com.backwell.auth_server.dto.request.CreateUserRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.jpa.service.JpaUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/register")
public class AuthController {
    private final JpaUserService jpaUserService;

    @PostMapping
    public ResponseEntity<MessageResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        var response = jpaUserService.createLocalUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
