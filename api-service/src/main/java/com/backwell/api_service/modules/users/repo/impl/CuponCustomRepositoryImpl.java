package com.backwell.api_service.modules.users.repo.impl;

import com.backwell.api_service.modules.users.dto.CuponDTO;
import com.backwell.api_service.modules.users.dto.CuponPagedResponse;
import com.backwell.api_service.modules.users.dto.CuponSearchFilters;
import com.backwell.api_service.modules.users.entity.UserInfo_;
import com.backwell.api_service.modules.users.entity.cupon.Cupon;
import com.backwell.api_service.modules.users.entity.cupon.Cupon_;
import com.backwell.api_service.modules.users.repo.CuponCustomRepository;
import com.backwell.api_service.modules.users.spec.CuponSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CuponCustomRepositoryImpl implements CuponCustomRepository {
    private final EntityManager em;

    @Override
    public CuponPagedResponse getCupons(CuponSearchFilters filters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<CuponDTO> query = cb.createQuery(CuponDTO.class);
        Root<Cupon> root = query.from(Cupon.class);
        Predicate predicate = CuponSpecification.withFilters(filters).toPredicate(root, query, cb);

        query.select(cb.construct(
                CuponDTO.class,
                root.get(Cupon_.id),
                root.get(Cupon_.name),
                root.get(Cupon_.type),
                root.get(Cupon_.user).get(UserInfo_.uuid),
                root.get(Cupon_.decimalFactor),
                root.get(Cupon_.active),
                root.get(Cupon_.stackable),
                root.get(Cupon_.createdAt),
                root.get(Cupon_.usedAt)
        )).where(predicate);

        int pagSize = filters.pageSize() != null ? filters.pageSize()+1 : 51;

        List<CuponDTO> results = em.createQuery(query)
                .setMaxResults(pagSize)
                .getResultList();

        boolean hasNextPage = results.size() == pagSize;
        return new CuponPagedResponse(
                hasNextPage,
                results.subList(0, pagSize - 2));
    }
}
