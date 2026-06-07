package com.backwell.api_service.modules.products.controller;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.modules.products.controller.req.CreateProductRequest;
import com.backwell.api_service.modules.products.controller.req.UpdateProductInfoRequest;
import com.backwell.api_service.modules.products.controller.res.ProductDTO;
import com.backwell.api_service.modules.products.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
@Validated
public class ProductController {
    private final ProductService productService;

    @PostMapping("/")
    public ResponseEntity<ProductDTO> createProduct(
            @Valid @RequestBody CreateProductRequest req
    ) {
        ProductDTO response = productService.create(req);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/")
    public ResponseEntity<ProductDTO> getProductInfo(@NotNull @RequestParam UUID productId) {
        var response = productService.getInfo(productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/")
    public ResponseEntity<ProductDTO> updateProductInfo(
            @NotNull @RequestParam UUID productId,
            @Valid @RequestBody UpdateProductInfoRequest req
    ) {
        var response = productService.updateProductInfo(productId, req);
        return ResponseEntity.accepted().body(response);
    }

    @PatchMapping("/category-update")
    public ResponseEntity<ProductDTO> updateProductCategory(
            @NotNull @RequestParam UUID productId,
            @NotNull @RequestParam UUID newCategoryId
    ) {
        var response = productService.updateCategory(productId, newCategoryId);
        return ResponseEntity.accepted().body(response);
    }


}
