package com.ecommerce.controller;

import com.ecommerce.dto.category.CategoryRequest;
import com.ecommerce.dto.category.CategoryResponse;
import com.ecommerce.dto.category.SubCategoryRequest;
import com.ecommerce.dto.category.SubCategoryResponse;
import com.ecommerce.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Category", description = "Category and sub-category catalog. Reads are public; writes require ADMIN.")
public class CategoryController {

    private final CategoryService categoryService;

    // ---------------------------------------------------------
    // Category endpoints
    // ---------------------------------------------------------

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @SecurityRequirements // public read, overriding the global bearer requirement
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @SecurityRequirements
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------
    // SubCategory endpoints
    // ---------------------------------------------------------

    @PostMapping("/subcategories")
    public ResponseEntity<SubCategoryResponse> createSubCategory(
            @Valid @RequestBody SubCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createSubCategory(request));
    }

    @SecurityRequirements
    @GetMapping("/categories/{categoryId}/subcategories")
    public ResponseEntity<List<SubCategoryResponse>> getSubCategoriesByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.getSubCategoriesByCategory(categoryId));
    }

    @SecurityRequirements
    @GetMapping("/subcategories/{id}")
    public ResponseEntity<SubCategoryResponse> getSubCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getSubCategoryById(id));
    }

    @PutMapping("/subcategories/{id}")
    public ResponseEntity<SubCategoryResponse> updateSubCategory(
            @PathVariable Long id, @Valid @RequestBody SubCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateSubCategory(id, request));
    }

    @DeleteMapping("/subcategories/{id}")
    public ResponseEntity<Void> deleteSubCategory(@PathVariable Long id) {
        categoryService.deleteSubCategory(id);
        return ResponseEntity.noContent().build();
    }
}
