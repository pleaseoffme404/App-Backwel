package com.backwell.api_service.modules.discount.controller;

import com.backwell.api_service.modules.discount.controller.req.CreateDiscountRequest;
import com.backwell.api_service.modules.discount.controller.req.DiscountFilterParams;
import com.backwell.api_service.modules.discount.controller.req.DiscountTargetsDTO;
import com.backwell.api_service.modules.discount.controller.req.UpdateDiscountRequest;
import com.backwell.api_service.modules.discount.controller.res.DiscountDTO;
import com.backwell.api_service.modules.discount.controller.res.DiscountExtractDTO;
import com.backwell.api_service.modules.discount.enums.DiscountSortField;
import com.backwell.api_service.modules.discount.service.DiscountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/discounts")
@Validated
public class DiscountController {
    private final DiscountService discountService;

// Te Agregaría un method para traer los activos en este mismo momento, pero pues @no hay tiempo XD",
// llama a search con active now en true, lo demás en blanco
// y el programa te completa con lo demás. Just in case, Page<T> devuelve un montón de metadatos verbosos,
// recomiendo checar con postman que devuelve XD
//    @GetMapping("/active")
//    public ResponseEntity<Page<DiscountDTO>> listActiveDiscounts() {
//        var response = discountService.getActiveNow();
//        return ResponseEntity.ok(response);
//    }


    @GetMapping("/search")
    public ResponseEntity<Page<DiscountDTO>> searchDiscounts(
            @RequestParam(required = false) UUID discountId,

            @RequestParam(required = false) Boolean nowActive,
            @RequestParam(required = false) Boolean stackable,

            @RequestParam(required = false) BigDecimal decimalValueMin,
            @RequestParam(required = false) BigDecimal decimalValueMax,

            @RequestParam(required = false) List<UUID> targetingItems,
            @RequestParam(required = false) List<UUID> targetingProducts,
            @RequestParam(required = false) List<UUID> targetingCategories,

            @RequestParam(required = false) Instant startDateMin,
            @RequestParam(required = false) Instant startDateMax,

            @RequestParam(required = false) Instant endDateMin,
            @RequestParam(required = false) Instant endDateMax,

            @RequestParam(required = false) Instant createdAtMin,
            @RequestParam(required = false) Instant createdAtMax,

            @RequestParam(defaultValue = "createdAt") DiscountSortField sortField,
            @RequestParam(defaultValue = "ASC") String sortOrder,
            @Positive @RequestParam(defaultValue = "50") Integer size,
            @Positive @RequestParam(defaultValue = "1") Integer page
    ) {

        Sort.Direction direction = sortOrder.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        // Creamos el Record DTO agrupando todos los parámetros recibidos
        DiscountFilterParams filterDto = new DiscountFilterParams(
                discountId,
                nowActive,
                stackable,

                decimalValueMin,
                decimalValueMax,

                targetingItems,
                targetingProducts,
                targetingCategories,

                startDateMin,
                startDateMax,

                endDateMin,
                endDateMax,

                createdAtMin,
                createdAtMax,

                sortField,
                direction,
                size,
                page
        );

        var response = discountService.searchDiscounts(filterDto);
        return ResponseEntity.ok(response);
    }

    // Methods for a particular discount
    @GetMapping("/info")
    public ResponseEntity<DiscountExtractDTO> discountDetails(@RequestParam UUID discountId) {
        var response = discountService.getInfo(discountId);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/update")
    public ResponseEntity<DiscountExtractDTO> updateDiscountMetadata(
            @RequestParam UUID discountId,
            @Valid @RequestBody UpdateDiscountRequest req) {
        var response  = discountService.updateDiscountMetadata(discountId, req);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/add-target")
    public ResponseEntity<DiscountExtractDTO> addTargets(
            @RequestParam UUID discountId,
            @Valid @RequestBody DiscountTargetsDTO targets
    ) {
        var response = discountService.addTargets(discountId, targets);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/pop-targets")
    public ResponseEntity<DiscountExtractDTO> popTarget(
            @RequestParam UUID discountId,
            @Valid @RequestBody DiscountTargetsDTO targets
    ){
        var response = discountService.popTargets(discountId, targets);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/")
    public ResponseEntity<DiscountExtractDTO> createDiscount(@Valid @RequestBody CreateDiscountRequest req) {
        DiscountExtractDTO response = discountService.createDiscount(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
