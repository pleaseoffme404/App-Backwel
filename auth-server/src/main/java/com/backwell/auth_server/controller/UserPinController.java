package com.backwell.auth_server.controller;

import com.backwell.auth_server.dto.request.SetUpPinAuthenticationRequest;
import com.backwell.auth_server.dto.request.UpdatePinRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.jpa.service.JpaUserPinService;
import com.backwell.auth_server.resolver.CurrentUser;
import com.backwell.auth_server.security.user.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pin-authentication")
@RequiredArgsConstructor
public class UserPinController {
    private final JpaUserPinService userPinService;

    @PostMapping("/setup")
    public ResponseEntity<MessageResponse> setUpPinAuthentication(
            @CurrentUser UserDTO user,
            @Valid @RequestBody SetUpPinAuthenticationRequest request
    ) {
        var response = userPinService.setUpUserPin(user.uuid(), request.pin());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/update")
    public ResponseEntity<MessageResponse> updatePinAuthentication(
            @CurrentUser UserDTO user,
            @Valid @RequestBody UpdatePinRequest request
    ) {
        var response = userPinService.updateUserPin(user.uuid(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check")
    public ResponseEntity<MessageResponse> checkPinAuthentication(
            @CurrentUser UserDTO user,
            @Valid @RequestBody SetUpPinAuthenticationRequest request
    ) {
        userPinService.checkUserPin(user.uuid(), request.pin());
        return ResponseEntity.ok(new MessageResponse("Access Granted"));
    }
}
