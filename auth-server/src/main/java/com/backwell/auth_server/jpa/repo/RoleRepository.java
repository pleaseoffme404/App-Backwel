package com.backwell.auth_server.jpa.repo;

import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByRoleName(RoleName roleName);

    Optional<Role> findByRoleName(RoleName roleName);
    long countByRoleNameIn(Set<RoleName> roleNames);
}
