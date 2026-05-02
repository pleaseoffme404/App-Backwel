package com.backwell.api_service.modules.products.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class ProductController {
    /*
    * private final InventoryService inventoryService;

    @PostMapping("/category")
    public ResponseEntity<Categoria> categoria(@RequestBody CategoryDTO dto) {
        var response = inventoryService.addCategory(dto.name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/provider")
    public ResponseEntity<Proveedor> proveedor(@RequestBody ProviderDTO dto) {
        var response = inventoryService.addProveedor(dto.name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/product")
    public ResponseEntity<Producto> producto(@Valid@RequestBody ProductPostDTO dto) {
        final var names = dto.getAttributeNames();
        dto.getVariants().forEach(v -> {
            if (!names.equals(v.getVariantAttributes().keySet())) {
                throw new IllegalStateException("Variant attributes must match declared product Attributes");
            }
        });

        inventoryService.newProduct(dto);
        return ResponseEntity.ok(new Producto());
    }*/
}
