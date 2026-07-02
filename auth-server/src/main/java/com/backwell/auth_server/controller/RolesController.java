package com.backwell.auth_server.controller;

import com.backwell.auth_server.dto.internal.RoleSearchFilters;
import com.backwell.auth_server.dto.request.CreateRoleRequest;
import com.backwell.auth_server.dto.request.UpdateRoleRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.dto.response.RoleDTO;
import com.backwell.auth_server.jpa.service.JpaRoleService;
import com.backwell.auth_server.jpa.service.RoleSearchService;
import com.backwell.auth_server.resolver.CurrentUser;
import com.backwell.auth_server.security.user.UserDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolesController {
    private final JpaRoleService jpaRoleService;
    private final RoleSearchService roleSearchService;

    @GetMapping("/")
    @PreAuthorize("hasPermission('roles:read')")
    public ResponseEntity<PageImpl<RoleDTO>> getRoles(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Set<UUID> permissions,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "1") Integer pag,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        var response = roleSearchService.findRoles(new RoleSearchFilters(
                name,
                permissions,
                order,
                sortBy,
                pag,
                size
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/")
    @PreAuthorize("hasPermission('roles:create')")
    public ResponseEntity<RoleDTO> createRole(
            @CurrentUser UserDTO userDTO,
            @RequestBody CreateRoleRequest createRoleRequest
    ) {
        var response = jpaRoleService.createRole(
                userDTO.roleId(),
                userDTO.permissionNames(),
                createRoleRequest
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/update")
    @PreAuthorize("hasPermission('roles:update')")
    public ResponseEntity<RoleDTO> updateRole(
            @CurrentUser UserDTO userDTO,
            @RequestParam @NotNull UUID targetRoleId,
            @RequestBody @Valid UpdateRoleRequest req
    ) {
        var response = jpaRoleService.updateRole(
                userDTO.roleId(),
                userDTO.permissionNames(),
                targetRoleId,
                req
        );

        return ResponseEntity.ok(response);
    }



    /* Mismo endpoint tanto para asignación como para revocación, ya que una revocación es un
    * caso particular de una asignación de rol con menos permisos*/
    @PostMapping("/update-user-role")
    @PreAuthorize("hasAllPermissions({'roles:assign','roles:revoke'})")
    public ResponseEntity<MessageResponse> updateUserRole (
            @CurrentUser UserDTO userDTO,
            @RequestParam @NotNull UUID targetUserId,
            @RequestParam @NotNull UUID targetRoleId
    ) {
        var response = jpaRoleService.updateUserRole(
                userDTO.roleId(),
                userDTO.permissionNames(),
                targetUserId,
                targetRoleId
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke-user")
    @PreAuthorize("hasPermission('roles:revoke')")
    public ResponseEntity<MessageResponse> revokeUser(
            @CurrentUser UserDTO userDTO,
            @RequestParam @NotNull UUID targetUserId
    ) {
        var response = jpaRoleService.revokeUser(
                userDTO.roleId(),
                userDTO.permissionNames(),
                targetUserId
        );

        return ResponseEntity.ok(response);

    }

}
