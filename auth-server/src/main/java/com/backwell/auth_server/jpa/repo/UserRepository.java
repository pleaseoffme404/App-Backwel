package com.backwell.auth_server.jpa.repo;

import com.backwell.auth_server.jpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {


    @NonNull
    Optional<User> findById(@NonNull UUID id);

    @NonNull
    Optional<User> findByEmail(@NonNull String email);
    boolean existsByEmail(String email);


    @Query(value = """
SELECT EXISTS(
    SELECT 1
    FROM users u
    WHERE u.role_id = :ownerRoleId
)""", nativeQuery = true)
    boolean existsUsersWithRole(@Param("ownerRoleId") UUID ownerRoleId);


    @Query("SELECT COUNT (u) > 1 FROM User u JOIN u.role r WHERE r.name = :name")
    boolean existsAtLeasTwoWithRole(@Param("name") String name);

    default User getOrThrow(@NonNull UUID id) {
        return findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with id:  '%s'".formatted(id)));
    }
}
