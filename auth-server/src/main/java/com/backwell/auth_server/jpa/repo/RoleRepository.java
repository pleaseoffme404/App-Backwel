package com.backwell.auth_server.jpa.repo;

import com.backwell.auth_server.exception.role.UnknownRoleException;
import com.backwell.auth_server.jpa.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    @NonNull
    Optional<Role> findById(@NonNull UUID id);

    @NonNull
    Optional<Role> findByName(@NonNull String name);

    default Role getOrThrow(@NonNull UUID id) {
        return findById(id)
                .orElseThrow(() -> new UnknownRoleException(
                        "Requested Role with Id: '%s' was not found.".formatted(id))
                );
    }

    default boolean violatesUniqueName(@NonNull String roleName) {
        return findByName(roleName).isPresent();
    }
}
