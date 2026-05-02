package com.backwell.auth_server.jpa.repo;

import com.backwell.auth_server.jpa.entity.User;
import com.backwell.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {


    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    boolean existsUserWithRole(@Param("roleName") RoleName roleName);

    @Query("SELECT COUNT(u) >1 FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    boolean exitsAtLeastTwoWithRole(@Param("roleName") RoleName roleName);

}
