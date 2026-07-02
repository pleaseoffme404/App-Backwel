package com.backwell.auth_server.security.roles;

import com.backwell.auth_server.init.RequiredRolesCache;
import com.backwell.enums.PermissionName;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAuthorizationPolicyEngine {
    private final RequiredRolesCache rolesCache;



    /**
     * Valida si un actor tiene permitido crear, modificar o asignar un rol
     * basado en el set de permisos que dicho rol contiene bajo políticas jurisdiccionales.
     *
     * @param actorRoleId El rol actual del usuario que ejecuta la acción.
     * @param actorPermissions Colección de permisos extraídos del token del actor.
     * @param targetPermissions Conjunto de permisos que se pretenden otorgar/configurar en el rol destino.
     * @throws AccessDeniedException Si se viola alguna regla jerárquica o de aislamiento de dominio.
     */
    public void validateAssignmentPolicy(
            UUID actorRoleId,
            Collection<PermissionName> actorPermissions,
            Set<PermissionName> targetPermissions
    ) {
        // REGLA 0: El rol OWNER tiene bypass absoluto, determinista e inmediato
        if (rolesCache.isOwner(actorRoleId)) {
            log.debug("Root OWNER bypass applied successfully to identity assignment transaction.");
            return;
        }

        // REGLA 1: Protección del Permiso Soberano Maestre
        // Nadie que no sea el OWNER puede otorgar o crear roles que contengan 'roles:meta:permission'
        if (targetPermissions.contains(PermissionName.ROLES_META_PERMISSION)) {
            log.error("Security Violation: Non-owner actor attempted to delegate or manipulate 'roles:meta:permission'.");
            throw new AccessDeniedException("Operación denegada: Solo el OWNER del sistema puede delegar el control soberano de la seguridad.");
        }

        // REGLA 2: Privilegio del Meta-Creador Estratégico (RRHH / Administración de Identidades)
        // Si el usuario cuenta con la llave maestra 'roles:meta:permission', puede forjar estructuras inter-jurisdiccionales
        if (actorPermissions.contains(PermissionName.ROLES_META_PERMISSION)) {
            log.debug("Strategic Meta-Permission authorization granted for this design transaction.");
            return;
        }

        // REGLA 3: Principio de Contención Estricta para Administradores de Área (roles:create / roles:assign-user)
        // Un operador común jamás puede otorgar facultades operativas que él mismo no posea en su token.
        if (!actorPermissions.containsAll(targetPermissions)) {
            log.warn("Policy Violation: Actor attempted to grant permissions outside their own domain.");
            throw new AccessDeniedException("Operación denegada: No puedes otorgar privilegios que tú mismo no posees.");
        }

        // REGLA 4: Control de Fronteras (Bloqueo de herencia estructural y validación estricta de Jurisdicciones)
        Set<String> actorJurisdictions = getJurisdictions(actorPermissions);

        for (PermissionName requestedPerm : targetPermissions) {

            // Un mánager operativo común NO puede inyectar ni crear delimitadores espaciales (:jurisdiction)
            if (requestedPerm.isJurisdiction()) {
                log.warn("Policy Violation: Non-meta actor attempted to delegate a Jurisdiction constraint [{}].", requestedPerm.getValue());
                throw new AccessDeniedException("Operación denegada: La asignación de Jurisdicciones es exclusiva del nivel estratégico.");
            }

            // Un mánager operativo tampoco puede inyectar otros metapermisos de acción secundarios (roles:*)
            if (requestedPerm.isActionMetaPermission()) {
                log.warn("Policy Violation: Non-meta actor attempted to delegate an Action Meta-Permission [{}].", requestedPerm.getValue());
                throw new AccessDeniedException("Operación denegada: No tienes autorización para delegar Metapermisos de Acción.");
            }

            // Validación dinámica de la frontera de la Jurisdicción asignada
            String domainNamespace = requestedPerm.getBaseResource(); // Ej: "product", "sale"

            boolean isInsideActorJurisdiction = actorJurisdictions.contains(domainNamespace);

            if (!isInsideActorJurisdiction) {
                log.warn("Policy Violation: Actor lacks Jurisdiction clearance for the requested domain namespace [{}].", domainNamespace);
                throw new AccessDeniedException(
                        String.format("Operación denegada: Tu cuenta carece de la Jurisdicción requerida sobre el dominio '%s'.", domainNamespace)
                );
            }
        }

        log.debug("Role assignment policy validation passed successfully.");
    }

    /**
     * @param actorRoleId El rol actual del usuario ejecutor.
     * @param actorPermissions Permisos en el token del usuario ejecutor.
     * @param targetRoleFullPermissions El total de permisos que posee el rol antes de ser alterado.
     * @param toRevoke Conjunto de permisos específicos que se le van a quitar.
     */
    public void validateRevocationPolicy(
            UUID actorRoleId,
            Collection<PermissionName> actorPermissions,
            Set<PermissionName> targetRoleFullPermissions,
            Set<PermissionName> toRevoke
    ) {
        if (rolesCache.isOwner(actorRoleId) || actorPermissions.contains(PermissionName.ROLES_META_PERMISSION)) {
            log.debug("Top-tier authorization granted for role revocation.");
            return;
        }

        boolean containsStructuralConstraints = targetRoleFullPermissions.stream()
                .anyMatch(p -> p.isMetaPermission() || p.isJurisdiction() || p.isActionMetaPermission());

        if (containsStructuralConstraints) {
            log.error("Security Violation: Operator attempted to modify a structural or high-privilege role.");
            throw new AccessDeniedException("Operación denegada: No puedes modificar ni revocar roles jerárquicamente superiores o de control estructural.");
        }

        // El resto de la validación por jurisdicciones opera correctamente sobre el delta (toRevoke)
        Set<String> jurisdictions = getJurisdictions(actorPermissions);

        for (PermissionName perm : toRevoke) {
            String domainNamespace = perm.getBaseResource();
            boolean isInsideActorJurisdiction = jurisdictions.contains(domainNamespace);

            if (!isInsideActorJurisdiction) {
                log.warn("Policy Violation: Operator attempted to revoke access from an alien domain namespace [{}].", domainNamespace);
                throw new AccessDeniedException(
                        String.format("Operación denegada: No tienes facultad jurisdiccional para alterar o revocar permisos del dominio '%s'.", domainNamespace)
                );
            }
        }

        log.debug("Role revocation policy validation passed successfully.");
    }


    @NonNull
    private Set<String> getJurisdictions(Collection<PermissionName> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            log.debug("Permission reference validation context contains null values.");
            return Set.of();
        }

        return permissions.stream()
                .filter(PermissionName::isJurisdiction)
                .map(PermissionName::getBaseResource)
                .collect(Collectors.toSet());
    }
}