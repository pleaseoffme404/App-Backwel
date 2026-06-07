package com.backwell.api_service.modules.users.repo;

import com.backwell.api_service.modules.users.entity.credit.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditTransactionRepository  extends JpaRepository<CreditTransaction, UUID> {
    @NonNull
    Optional<CreditTransaction> findById(@NonNull UUID id);
}
