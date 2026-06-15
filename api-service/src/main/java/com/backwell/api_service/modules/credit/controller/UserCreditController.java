package com.backwell.api_service.modules.credit.controller;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.config.user.annotation.StaffLevel;
import com.backwell.api_service.modules.credit.controller.req.UpdateCreditRequest;
import com.backwell.api_service.modules.credit.controller.res.CreditBalanceDTO;
import com.backwell.api_service.modules.credit.service.CreditsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
@Validated
public class UserCreditController {
    private final CreditsService creditsService;

    @GetMapping("/")
    public ResponseEntity<CreditBalanceDTO> getCreditBalanceForUser(UserSession session) {
        var response = creditsService.getUserCreditBalance(session);
        return ResponseEntity.ok(response);
    }



    @StaffLevel
    @PostMapping("/")
    public ResponseEntity<CreditBalanceDTO> updateUserCreditBalance(
            UserSession session,
            @Valid @RequestBody UpdateCreditRequest req
    ) {
        var response = creditsService.updateUserCreditBalance(session, req);
        return ResponseEntity.ok(response);
    }
}
