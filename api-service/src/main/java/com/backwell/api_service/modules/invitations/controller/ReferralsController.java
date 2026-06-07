package com.backwell.api_service.modules.invitations.controller;


import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.modules.invitations.service.InvitationService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/referrals")
@RequiredArgsConstructor
public class ReferralsController {
    private final InvitationService invitationService;

    @PostMapping("/invitation")
    public ResponseEntity<?> invite(
            UserSession session,
            @RequestParam @NotNull @Email String targetEmail
    ) {
        var response = invitationService.createInvitation(session.uuid(), targetEmail);

        return ResponseEntity.ok(response);
    }
}
