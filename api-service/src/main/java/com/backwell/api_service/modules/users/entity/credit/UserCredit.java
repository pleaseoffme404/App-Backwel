package com.backwell.api_service.modules.users.entity.credit;

import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class UserCredit {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_credit_seq")
    @SequenceGenerator(
            name = "user_credit_seq",
            sequenceName = "user_credit_seq"
    )
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "used_id", nullable = false, updatable = false)
    private UserInfo userInfo;

    @Setter
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;


    @Column(nullable = false)
    private Instant lastUpdated;

    @PrePersist
    protected void onCreate() {
        lastUpdated = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }

    public UserCredit(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
