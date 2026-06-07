package com.backwell.api_service.modules.discount.jpa.spec;

import com.backwell.api_service.modules.discount.controller.req.DiscountFilterParams;
import com.backwell.api_service.modules.discount.jpa.entity.Discount;
import com.backwell.api_service.modules.discount.jpa.entity.DiscountTarget;
import com.backwell.api_service.modules.discount.jpa.entity.DiscountTarget_;
import com.backwell.api_service.modules.discount.jpa.entity.Discount_;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiscountSpecification {

    public static Specification<Discount> filterByParams(DiscountFilterParams filter) {
        return (root, query, cb) -> {
            Assert.notNull(query, "Query must not be null");
            List<Predicate> predicates = new ArrayList<>();

            // 1. UUID Equal
            filter.getDiscountId().ifPresent(id ->
                    predicates.add(cb.equal(root.get(Discount_.ID), id)));

            // 2. Booleans Equal
            filter.getStackable().ifPresent(stackable ->
                    predicates.add(cb.equal(root.get(Discount_.stackable), stackable)));

            // 3. Lógica compleja: Now Active (startDate <= NOW <= endDate)
            filter.getNowActive().ifPresent(nowActive -> {
                Instant now = Instant.now();
                if (nowActive) {
                    predicates.add(cb.lessThanOrEqualTo(root.get(Discount_.startDate), now));
                    predicates.add(cb.greaterThanOrEqualTo(root.get(Discount_.endDate), now));
                } else {
                    // Inactivo: O no ha empezado, o ya terminó
                    predicates.add(cb.or(
                            cb.greaterThan(root.get(Discount_.startDate), now),
                            cb.lessThan(root.get(Discount_.endDate), now)
                    ));
                }
            });

            // 4. Ranges (BigDecimals)
            filter.getDecimalValueMin().ifPresent(min ->
                    predicates.add(cb.ge(root.get(Discount_.decimalValue), min)));
            filter.getDecimalValueMax().ifPresent(max ->
                    predicates.add(cb.le(root.get(Discount_.decimalValue), max)));

            // 5. Ranges (Instants)
            filter.getStartDateMin().ifPresent(min ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get(Discount_.startDate), min)));
            filter.getStartDateMax().ifPresent(max ->
                    predicates.add(cb.lessThanOrEqualTo(root.get(Discount_.startDate), max)));

            filter.getEndDateMin().ifPresent(min ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get(Discount_.endDate), min)));
            filter.getEndDateMax().ifPresent(max ->
                    predicates.add(cb.lessThanOrEqualTo(root.get(Discount_.endDate), max)));

            filter.getCreatedAtMin().ifPresent(min ->
                    predicates.add(cb.greaterThanOrEqualTo(root.get(Discount_.createdAt), min)));
            filter.getCreatedAtMax().ifPresent(max ->
                    predicates.add(cb.lessThanOrEqualTo(root.get(Discount_.createdAt), max)));


            filter.getTargetingItems().ifPresent(items -> {
                if (!items.isEmpty()) {
                    jakarta.persistence.criteria.Join<Discount, DiscountTarget> joinTarget = root.join(Discount_.targets);

                    predicates.add(cb.and(
                            joinTarget.get(DiscountTarget_.itemId).in(items),
                            cb.isNotNull(joinTarget.get(DiscountTarget_.itemId))
                    ));
                }
            });

            filter.getTargetingProducts().ifPresent(products -> {
                if (!products.isEmpty()) {
                    jakarta.persistence.criteria.Join<Discount, DiscountTarget> joinTarget = root.join(Discount_.targets);

                    predicates.add(cb.and(
                            joinTarget.get(DiscountTarget_.productId).in(products),
                            cb.isNotNull(joinTarget.get(DiscountTarget_.productId))
                    ));
                }
            });

            filter.getTargetingCategories().ifPresent(categories -> {
                if (!categories.isEmpty()) {
                    jakarta.persistence.criteria.Join<Discount, DiscountTarget> joinTarget = root.join(Discount_.targets);

                    predicates.add(cb.and(
                            joinTarget.get(DiscountTarget_.categoryId).in(categories),
                            cb.isNotNull(joinTarget.get(DiscountTarget_.categoryId))
                    ));
                }
            });
            query.distinct(true);

            // Combinar todos los predicados usando un AND lógico
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
