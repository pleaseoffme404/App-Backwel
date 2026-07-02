package com.backwell.auth_server.jpa.service;

import com.backwell.auth_server.dto.internal.RoleSearchFilters;
import com.backwell.auth_server.dto.response.PermissionDTO;
import com.backwell.auth_server.dto.response.RoleDTO;
import com.backwell.auth_server.init.PermissionsCache;
import com.backwell.auth_server.jpa.entity.Permission;
import com.backwell.auth_server.jpa.entity.Permission_;
import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.auth_server.jpa.entity.Role_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleSearchService {
    private final EntityManager entityManager;
    private final PermissionsCache permissionsCache;

    @Transactional(readOnly = true)
    public PageImpl<RoleDTO> findRoles(RoleSearchFilters filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Inicializar valores de paginación por defecto si vienen nulos
        Pageable pageable = PageRequest.of(filters.getPag(), filters.getSize());


        CriteriaQuery<Role> dataQuery = cb.createQuery(Role.class);
        Root<Role> roleRoot = dataQuery.from(Role.class);

        roleRoot.fetch(Role_.permissions, JoinType.LEFT);

        List<Predicate> dataPredicates = new ArrayList<>();

        // Filtro por Nombre
        filters.getName().ifPresent(name -> {
            String pattern = "%" + name.toLowerCase() + "%";
            dataPredicates.add(cb.like(cb.lower(roleRoot.get(Role_.name)), pattern));
        });

        filters.getPermissions().ifPresent(permissions -> {
            if (!permissions.isEmpty()) {
                permissionsCache.checkExistById(permissions);

                addPermissionsSubqueryPredicates(dataQuery, roleRoot, dataPredicates, permissions);
            }
        });

        dataQuery.select(roleRoot)
                .distinct(true)
                .where(dataPredicates.toArray(new Predicate[0]));

        String sortBy = (filters.getSortBy() != null) ? filters.getSortBy() : "name";
        String orderDir = (filters.getOrder() != null) ? filters.getOrder() : "asc";

        if ("desc".equalsIgnoreCase(orderDir)) {
            dataQuery.orderBy(cb.desc(roleRoot.get(sortBy)));
        } else {
            dataQuery.orderBy(cb.asc(roleRoot.get(sortBy)));
        }


        TypedQuery<Role> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Role> rolesList = typedQuery.getResultList();


        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Role> countRoot = countQuery.from(Role.class);

        List<Predicate> countPredicates = new ArrayList<>();

        filters.getName().ifPresent(name -> {
            String pattern = "%" + name.toLowerCase() + "%";
            countPredicates.add(cb.like(cb.lower(countRoot.get(Role_.name)), pattern));
        });

        // Replicamos el filtro de Permisos para el conteo
        filters.getPermissions().ifPresent(permissions -> {
            if (!permissions.isEmpty()) {
                addPermissionsSubqueryPredicates(countQuery, countRoot, countPredicates, permissions);
            }
        });

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        List<RoleDTO> dtoList = rolesList.stream()
                .map(role -> new RoleDTO(
                        role.getId(),
                        role.getName(),
                        role.getPermissions().stream()
                                .map(p -> new PermissionDTO(
                                        p.getId(),
                                        p.getPermissionNameString())
                                ).toList()
                ))
                .toList();

        return new PageImpl<>(dtoList, pageable, totalElements);
    }

    private void addPermissionsSubqueryPredicates(
            CriteriaQuery<?> query,
            Root<Role> root,
            List<Predicate> predicates,
            Set<UUID> permissions
    ) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<Role> countSubRoleRoot = subquery.from(Role.class);
        Join<Role, Permission> countSubPermissionJoin = countSubRoleRoot.join(Role_.permissions);

        subquery.select(countSubRoleRoot.get(Role_.id))
                .where(countSubPermissionJoin.get(Permission_.id).in(permissions));

        predicates.add(root.get(Role_.id).in(subquery));
    }
}
