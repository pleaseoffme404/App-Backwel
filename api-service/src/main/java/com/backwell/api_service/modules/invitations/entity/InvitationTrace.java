package com.backwell.api_service.modules.invitations.entity;


import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class InvitationTrace {
    @Id
    private UUID id;


    @ManyToOne(fetch = FetchType.EAGER,  optional = false)
    @JoinColumn(name = "inviting_id", nullable = false, updatable = false)
    private UserInfo invitingUser;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "invited_uuid", nullable = false, updatable = false)
    private UserInfo invitedUser;

    @Column(nullable = false)
    private String invitationCode;

    @Column(nullable = false, updatable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @Setter
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant burnedAt;


    public InvitationTrace(UUID id, UserInfo invitingUser, UserInfo invitedUser, String invitationCode) {
        this.id = id;
        this.invitingUser = invitingUser;
        this.invitedUser = invitedUser;
        this.invitationCode = invitationCode;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
