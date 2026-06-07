package com.backwell.api_service.modules.users.repo;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.modules.users.entity.credit.UserCredit;
import io.micrometer.common.lang.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCreditRepository  extends JpaRepository<UserCredit, Long> {
    @NonNull
    Optional<UserCredit> findById(@NonNull Long id);

    @NonNull
    Optional<UserCredit> findByUserInfo_Uuid(@NonNull UUID userId);


    default UserCredit getOrThrow(@NonNull UUID userId) {
        return this.findByUserInfo_Uuid(userId)
                .orElseThrow(() -> new SystemException("User credit not found."));
    }
}
