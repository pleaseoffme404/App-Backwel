package com.backwell.api_service.modules.products.controller;


import com.backwell.api_service.modules.products.controller.req.CreateCategoryGraphRequest;
import com.backwell.api_service.modules.products.controller.req.CreateCategoryRequest;
import com.backwell.api_service.modules.products.controller.req.RenameCategoryRequest;
import com.backwell.api_service.modules.products.controller.res.CategoryNodeDTO;
import com.backwell.api_service.common.dto.MessageResponse;
import com.backwell.api_service.modules.products.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;


    @GetMapping("/")
    public ResponseEntity<List<CategoryNodeDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.fetchAll());
    }

    @PostMapping("/")
    public ResponseEntity<CategoryNodeDTO> createCategory(@RequestBody @Valid CreateCategoryRequest req) {
        var response = categoryService.addCategory(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/graph")
    public ResponseEntity<List<CategoryNodeDTO>> createCategoryGraph(@RequestBody @Valid CreateCategoryGraphRequest req) {
        var response = categoryService.createCategoryGraph(req);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/rename")
    public ResponseEntity<CategoryNodeDTO> renameCategory(@Valid @RequestBody RenameCategoryRequest req) {
        var response = categoryService.renameCategory(req);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/reubicate")
    public ResponseEntity<CategoryNodeDTO> reubicateCategory(
            @RequestParam UUID targetId,
            @RequestParam UUID newParentId
    ) {
        var response = categoryService.updateCategoryParentId(targetId, newParentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/")
    public ResponseEntity<MessageResponse> deleteCategory(
            HttpServletRequest request,
            @RequestParam UUID categoryId) {
        String message = categoryService.attemptCategoryDelete(categoryId);
        var response = new MessageResponse(message, request.getRequestURI(), HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }


}
