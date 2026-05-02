package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.modules.products.dto.PictureExtract;
import com.backwell.api_service.modules.products.jpa.entity.prod.ItemPicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
public interface ItemPictureRepository extends JpaRepository<ItemPicture, UUID> {

    @Query("""
SELECT new com.backwell.api_service.modules.products.dto.PictureExtract (
    i.id,
    p.url,
    p.order
)
FROM ItemPicture p
JOIN p.item i
WHERE i.id IN :ids
ORDER BY p.order
""")
    List<PictureExtract> findImagesForVariants(@Param("ids") List<UUID> ids);

    default Map<UUID, List<String>> findImagesMapForVariants(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }
        int batchSize = 500;
        return IntStream.range(0, (ids.size() + batchSize - 1) / batchSize)
                .mapToObj(i -> ids.subList(i * batchSize, Math.min(ids.size(), (i + 1) * batchSize)))
                .flatMap(batch -> findImagesForVariants(batch).stream())
                .collect(Collectors.groupingBy(
                        PictureExtract::itemId,
                        Collectors.mapping(PictureExtract::url, Collectors.toList())
                ));
    }
}

