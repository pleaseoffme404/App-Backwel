package com.backwell.auth_server.jpa.repo;

import com.backwell.auth_server.jpa.entity.Permission;
import com.backwell.enums.PermissionName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    @NonNull
    Optional<Permission> findById(@NonNull UUID id);

    @Query(value = "SELECT permission_name from permission", nativeQuery = true)
    List<String> getDbPermissionNames();

    @Modifying
    @Query(value = "DELETE FROM permission WHERE permission_name = :obsoletePermission", nativeQuery = true)
    void deleteObsoletePermission(@Param("obsoletePermission") String obsoletePermission);


    Optional<Permission> findByPermissionName(PermissionName permissionName);
    List<Permission> findAllByPermissionNameIn(Collection<PermissionName> permissionNames);
}
