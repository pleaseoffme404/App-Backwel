package com.backwell.api_service.modules.invitations;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;


@Data
@Builder
public class InvitationMetadata implements Serializable {
    private UUID invitingUserId;
    private String invitedEmail;
    private Instant createdAt;
}
