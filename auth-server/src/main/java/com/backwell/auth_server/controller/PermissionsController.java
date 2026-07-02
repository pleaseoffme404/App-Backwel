package com.backwell.auth_server.controller;

import com.backwell.auth_server.dto.response.PermissionDTO;
import com.backwell.auth_server.init.PermissionsCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionsController {
    private final PermissionsCache permissionsCache;

    @GetMapping("/")
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        List<PermissionDTO> response = permissionsCache.getAll().stream().map(PermissionDTO::fromEntity).toList();
        return ResponseEntity.ok(response);
    }

}
