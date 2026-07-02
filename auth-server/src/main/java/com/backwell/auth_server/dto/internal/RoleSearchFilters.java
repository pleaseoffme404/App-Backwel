package com.backwell.auth_server.dto.internal;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class RoleSearchFilters {
    private final String name;
    private final Set<UUID> permissions;
    private final String order;
    private final String sortBy;
    private final Integer pag;
    private final Integer size;


    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<Set<UUID>> getPermissions() {
        return Optional.ofNullable(permissions);
    }


}
