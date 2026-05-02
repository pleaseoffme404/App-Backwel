package com.backwell.api_service.modules.users.repo;

import com.backwell.api_service.modules.users.dto.UserAddressDTO;
import com.backwell.api_service.modules.users.entity.UserAddress;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    @NotNull Optional<UserAddress> findById(@NotNull Long id);
    List<UserAddress> findByUser_Uuid(UUID userId);

    int deleteByUser_UuidAndSlotIndex(@NotNull UUID userId, int slotIndex);

    Optional<UserAddress> findByUser_UuidAndSlotIndex(@NotNull UUID userId, int slotIndex);

    @Query("""
SELECT new com.backwell.api_service.modules.users.dto.UserAddressDTO(
ua.internalName,
ua.slotIndex,
ua.googleAddress
)
FROM UserAddress ua
WHERE ua.user.uuid = :userId""")
    List<UserAddressDTO> fetchForUser(UUID userId);
}
