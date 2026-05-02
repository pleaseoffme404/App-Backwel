package com.backwell.api_service.modules.users.controller;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.modules.users.dto.CompleteAccountRequest;
import com.backwell.api_service.modules.users.dto.UpdateUserInfoRequest;
import com.backwell.api_service.modules.users.dto.UserInfoDTO;
import com.backwell.api_service.modules.users.service.UserInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserInfoService userInfoService;

    @PostMapping("/complete-account")
    public ResponseEntity<UserInfoDTO> completeAccount(
            UserSession userSession,
            @RequestBody @Valid CompleteAccountRequest request
    ) {
        var response = userInfoService.completeAccount(userSession, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @PatchMapping("/update")
    public ResponseEntity<UserInfoDTO> updateUserInfo(
            UserSession userSession,
            @RequestBody @Valid UpdateUserInfoRequest request
    ) {
        var response = userInfoService.updateAccount(userSession, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/userinfo")
    public ResponseEntity<UserInfoDTO> getUserInfo(UserSession userSession) {
        UserInfoDTO response = userInfoService.info(userSession);
        return ResponseEntity.ok(response);
    }
}
