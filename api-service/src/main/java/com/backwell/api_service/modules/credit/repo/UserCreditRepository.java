package com.backwell.api_service.modules.credit.repo;

import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.modules.credit.controller.res.CreditBalanceDTO;
import com.backwell.api_service.modules.credit.entity.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCreditRepository  extends JpaRepository<UserCredit, Long> {
    @NonNull
    Optional<UserCredit> findById(@NonNull Long id);

    @NonNull
    Optional<UserCredit> findByUserInfo_Uuid(@NonNull UUID userId);


    @Query("""
SELECT NEW com.backwell.api_service.modules.credit.controller.res.CreditBalanceDTO(
c.userId,
c.balance
)
FROM UserCredit c
WHERE c.userId = :userId""")
    Optional<CreditBalanceDTO> getUserBalance(@NonNull UUID userId);

    default CreditBalanceDTO getBalanceOrThrow(@NonNull UUID userId) {
        return getUserBalance(userId)
                .orElseThrow(() -> new SystemException("User credit balance not found."));
    }

    default UserCredit getOrThrow(@NonNull UUID userId) {
        return this.findByUserInfo_Uuid(userId)
                .orElseThrow(() -> new SystemException("User credit not found."));
    }
}
