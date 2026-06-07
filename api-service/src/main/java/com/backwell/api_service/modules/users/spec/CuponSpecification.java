package com.backwell.api_service.modules.users.spec;

import com.backwell.api_service.modules.users.dto.CuponSearchFilters;
import com.backwell.api_service.modules.users.entity.cupon.Cupon;
import com.backwell.api_service.modules.users.entity.cupon.Cupon_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CuponSpecification {

    public static Specification<Cupon> withFilters(CuponSearchFilters filters) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.optLastId().ifPresent(val ->
                    predicates.add(cb.greaterThan(root.get(Cupon_.id), val)));

            filters.optNameLike().ifPresent(val ->
                    predicates.add(cb.like(cb.lower(root.get(Cupon_.name)), "%" + val.toLowerCase() + "%")));

            filters.optType().ifPresent(val ->
                    predicates.add(cb.equal(root.get(Cupon_.type), val)));

            filters.optTargetId().ifPresent(val ->
                    predicates.add(cb.equal(root.get(Cupon_.user).get("id"), val)));

            // Filtros de Rangos (Decimales)
            filters.optPercentageMin().ifPresent(val ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get(Cupon_.decimalFactor), val)));

            filters.optPercentageMax().ifPresent(val ->
                    predicates.add(cb.lessThanOrEqualTo(root.get(Cupon_.decimalFactor), val)));

            // Filtros Booleanos
            filters.optActive().ifPresent(val ->
                    predicates.add(cb.equal(root.get(Cupon_.active), val)));

            filters.optStackable().ifPresent(val ->
                    predicates.add(cb.equal(root.get(Cupon_.stackable), val)));

            // Filtros de Fecha (Instant)
            filters.optCreatedAtMin().ifPresent(val ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get(Cupon_.createdAt), val)));

            filters.optCreatedAtMax().ifPresent(val ->
                    predicates.add(cb.lessThanOrEqualTo(root.get(Cupon_.createdAt), val)));

            filters.optUsedAtMin().ifPresent(val ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get(Cupon_.usedAt), val)));

            filters.optUsedAtMax().ifPresent(val ->
                    predicates.add(cb.lessThanOrEqualTo(root.get(Cupon_.usedAt), val)));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
