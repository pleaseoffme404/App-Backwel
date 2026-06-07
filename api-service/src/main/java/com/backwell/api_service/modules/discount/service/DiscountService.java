package com.backwell.api_service.modules.discount.service;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.discount.controller.req.CreateDiscountRequest;
import com.backwell.api_service.modules.discount.controller.req.DiscountFilterParams;
import com.backwell.api_service.modules.discount.controller.req.DiscountTargetsDTO;
import com.backwell.api_service.modules.discount.controller.req.UpdateDiscountRequest;
import com.backwell.api_service.modules.discount.controller.res.DiscountDTO;
import com.backwell.api_service.modules.discount.controller.res.DiscountExtractDTO;
import com.backwell.api_service.modules.discount.dto.CategoryDiscountExtract;
import com.backwell.api_service.modules.discount.dto.ProductDiscountExtract;
import com.backwell.api_service.modules.discount.jpa.entity.Discount;
import com.backwell.api_service.modules.discount.jpa.entity.DiscountTarget;
import com.backwell.api_service.modules.discount.jpa.repo.DiscountRepository;
import com.backwell.api_service.modules.discount.jpa.spec.DiscountSpecification;
import com.backwell.api_service.modules.products.dto.CategoryPath;
import com.backwell.api_service.modules.products.jpa.repo.CategoryRepository;
import com.backwell.api_service.modules.products.jpa.repo.ItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.backwell.api_service.common.exception.codes.DiscountErrorCode.*;

@Service
@RequiredArgsConstructor
public class DiscountService {
    private final DiscountRepository discountRepository;
    private final UUIDService uuidService;
    private final ItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;


    @Transactional
    public DiscountExtractDTO createDiscount(CreateDiscountRequest req) {
        DiscountTargetsDTO t = req.targets();

        Discount d = Discount.builder()
                .id(uuidService.next())
                .name(req.discountName())
                .decimalValue(req.discountDecimal())
                .decimalFactor(BigDecimal.ONE.subtract(req.discountDecimal()))
                .stackable(req.stackable())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .active(true)
                .build();


        t.getCategoryTargets().ifPresent(categories-> {
            if (categoryRepository.countExistingIds(categories) != categories.size()) {
                throw new IllegalArgumentException("One or more categories were not found");
            }

            categories.forEach(ctId -> d.addTarget(DiscountTarget
                    .builder()
                    .categoryId(ctId)
                    .build()
            ));
        });

        t.getProductTargets().ifPresent(products-> {
            if (productRepository.countExistingIds(products) != products.size()) {
                throw new IllegalArgumentException("One or more products were not found");
            }

            products.forEach(pId -> d.addTarget(DiscountTarget
                    .builder()
                    .productId(pId)
                    .build()
            ));
        });

        t.getItemTargets().ifPresent(items -> {
            if (itemRepository.countExistingIds(items) != items.size()) {
                throw new IllegalArgumentException("One or more items were not found");
            }

            items.forEach(itemId -> d.addTarget(DiscountTarget
                    .builder()
                    .itemId(itemId)
                    .build()));
        });

        Discount saved = discountRepository.saveAndFlush(d);
        return discountRepository.getDiscountDetails(saved.getId());
    }


    @Transactional
    public DiscountExtractDTO getInfo(UUID discountId) {
        return discountRepository.getDiscountDetails(discountId);
    }


    @Transactional
    public DiscountExtractDTO popTargets(UUID discountId, DiscountTargetsDTO targets) {
        discountRepository.popDiscountTargets(discountId, targets);
        return discountRepository.getDiscountDetails(discountId);
    }

    @Transactional
    public DiscountExtractDTO addTargets(UUID discountId, DiscountTargetsDTO req) {
        discountRepository.addDiscountTargets(discountId, req);
        return discountRepository.getDiscountDetails(discountId);
    }


    @Transactional
    public DiscountExtractDTO updateDiscountMetadata(UUID discountId, UpdateDiscountRequest req) {
        Discount d = discountRepository.getIfUpdatable(discountId)
                .orElseThrow(() -> new BusinessException(
                        "Discount with Id: `%s` was not found or is not updatable.".formatted(discountId),
                        NOT_UPDATABLE_DISCOUNT
                ));

        req.nameOptional().ifPresent(d::setName);
        req.activeOptional().ifPresent(d::setActive);
        req.decimalValueOptional().ifPresent(d::setDecimalValue);
        req.stackableOptional().ifPresent(d::setStackable);

        Instant currentStart = req.startDateOptional().orElse(d.getStartDate());
        Instant currentEnd = req.endDateOptional().orElse(d.getEndDate());
        Instant now = Instant.now().minusSeconds(10);

        req.startDateOptional().ifPresent(newStart -> {
            if (newStart.isBefore(now)) {
                throw new BusinessException("La nueva fecha de inicio no puede estar en el pasado.", INVALID_DATE_RANGE);
            }
            if (d.getStartDate().isBefore(now)) {
                throw new BusinessException("No se puede modificar la fecha de inicio de un descuento que ya comenzó.", DISCOUNT_ALREADY_STARTED);
            }
            d.setStartDate(newStart);
        });
        req.endDateOptional().ifPresent(newEnd -> {
            if (newEnd.isBefore(now)) {
                throw new BusinessException("La fecha de fin no puede estar en el pasado.", INVALID_DATE_RANGE);
            }
            d.setEndDate(newEnd);
        });

        if (currentStart.isAfter(currentEnd) || currentStart.equals(currentEnd)) {
            throw new BusinessException("La fecha de inicio debe ser estrictamente anterior a la fecha de fin.", INVALID_DATE_RANGE);
        }

        discountRepository.saveAndFlush(d);
        return discountRepository.getDiscountDetails(discountId);
    }


    @Transactional
    public CategoryDiscountExtract getForCategoryPath(CategoryPath path) {
        return discountRepository.resolveDiscountForCategory(path.idPath());
    }

    @Transactional
    public ProductDiscountExtract getDiscountForProduct(UUID productId, CategoryPath path) {
        return discountRepository.resolveDiscountForProduct(path.idPath(), productId);
    }

    // ...... For Get Information Methods... //
    @Transactional(readOnly = true)
    public Page<DiscountDTO> searchDiscounts(DiscountFilterParams params){
        Sort sort = Sort.by(params.direction(), params.sortField().getAttribute());
        Pageable pageable = PageRequest.of(params.pageNumber(),  params.pageSize(), sort);
        Specification<Discount> spec = DiscountSpecification.filterByParams(params);
        Page<Discount> discountPage = discountRepository.findAll(spec, pageable);
        return discountPage.map(this::convertToDTO);
    }

    private DiscountDTO convertToDTO(Discount d) {
        return DiscountDTO.builder()
                .discountId(d.getId())
                .name(d.getName())
                .decimalValue(d.getDecimalFactor())
                .stackable(d.isStackable())
                .active(d.isActive())
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
