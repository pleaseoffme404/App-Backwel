package com.backwell.api_service.modules.users.repo;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.users.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.backwell.api_service.common.exception.codes.UserErrorCode.USER_NOT_FOUND;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, UUID> {

    @Deprecated
    Optional<UserInfo> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUuid(UUID uuid);

    Optional<UserInfo> findByUuid(UUID userId);

    @Query("""
SELECT u
FROM UserInfo  u
WHERE u.uuid IN :ids
""")
    List<UserInfo> findWithIdIn(Set<UUID> ids);

    /**
     * Returns the entity or throws a {@link BusinessException} if user 404*/
    default UserInfo getOrThrow(UserSession session) {
        if (session == null || session.uuid() == null) {
            throw new BusinessException("Invalid session", USER_NOT_FOUND);
        }
        return getOrThrow(session.uuid());
    }

    default UserInfo getOrThrow(UUID uuid) {
        return findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(
                        "User with Id: [%s] was not found".formatted(uuid),
                        USER_NOT_FOUND
                ));
    }
}
