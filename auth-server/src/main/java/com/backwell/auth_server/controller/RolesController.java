package com.backwell.auth_server.controller;

import com.backwell.auth_server.dto.request.GrantRoleRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.jpa.service.JpaUserService;
import com.backwell.auth_server.resolver.CurrentUser;
import com.backwell.auth_server.security.user.UserDTO;
import com.backwell.enums.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolesController {
    private final JpaUserService jpaUserService;

    @PostMapping("/grant")
    public ResponseEntity<MessageResponse> grantRole (
            @RequestBody GrantRoleRequest request,
            @CurrentUser UserDTO userDTO
    ) {

        RoleName requestedRole = RoleName.fromString(request.roleName());

        Set<RoleName> roles = userDTO.roles()
                .stream()
                .map(RoleName::fromString)
                .collect(Collectors.toSet());

        RoleName highestRole = RoleName.getHighestRole(roles);

        if (!highestRole.canCreate(requestedRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        }
        var response =  jpaUserService.grantRole(request.email(), requestedRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<MessageResponse> revokeRole(
            @RequestBody GrantRoleRequest request,
            @CurrentUser UserDTO userDTO
    ) {
        RoleName requestedRole = RoleName.fromString(request.roleName());

        Set<RoleName> roles = userDTO.roles()
                .stream()
                .map(RoleName::fromString)
                .collect(Collectors.toSet());

        RoleName highestRole = RoleName.getHighestRole(roles);

        var response = jpaUserService.revokeRole(request.email(), requestedRole, highestRole);
        return ResponseEntity.ok(response);
    }

}
