package com.backwell.api_service.modules.users.repo;

import com.backwell.api_service.modules.users.dto.UserCuponDTO;
import com.backwell.api_service.modules.users.entity.cupon.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CuponRepository extends JpaRepository<Cupon, UUID>, CuponCustomRepository {

    @NonNull
    Optional<Cupon> findById(@NonNull UUID id);

    @Query("""
SELECT new com.backwell.api_service.modules.users.dto.UserCuponDTO(
c.id,
c.name,
c.type,
c.decimalFactor,
c.stackable
)
FROM Cupon c
WHERE c.user.uuid = :id
AND c.active = true
""")
    List<UserCuponDTO> findAvailableForUserId(@NonNull @Param("id") UUID id);
}
