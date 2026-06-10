package com.backwell.auth_server.jpa.repo;

import com.backwell.auth_server.jpa.entity.UserPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPinRepository extends JpaRepository<UserPin, UUID> {
    @Query("""
SELECT p
FROM UserPin p
WHERE p.userId = :userId""")
    Optional<UserPin> findForUserId(@Param("userId") UUID userId);
}
