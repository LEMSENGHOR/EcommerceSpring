package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.dto.product.ProductImageRequest;
import com.ecommerce.dto.product.ProductImageResponse;
import com.ecommerce.dto.product.ProductRequest;
import com.ecommerce.dto.product.ProductResponse;
import com.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Product catalog, images, and stock. Reads are public; writes require ADMIN.")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    /**
     * Paginated, filterable product listing.
     * Examples:
     *   GET /api/products?page=0&size=20&sort=price,asc
     *   GET /api/products?categoryId=3&minPrice=10&maxPrice=100
     *   GET /api/products?name=shirt&inStockOnly=true
     */
    @Operation(summary = "Search products",
            description = "All filter query params are optional and combinable: name (substring match), "
                    + "categoryId, subCategoryId, brandId, minPrice, maxPrice, status (ACTIVE/INACTIVE), "
                    + "inStockOnly. Standard Spring pagination params also apply: page, size, sort "
                    + "(e.g. sort=price,desc).")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean inStockOnly,
            @PageableDefault(size = 20) Pageable pageable) {

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .name(name)
                .categoryId(categoryId)
                .subCategoryId(subCategoryId)
                .brandId(brandId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .status(status)
                .inStockOnly(inStockOnly)
                .build();

        return ResponseEntity.ok(productService.searchProducts(filter, pageable));
    }

    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(summary = "Delete a product",
            description = "Blocked with 409 if the product has ever been ordered (order history is "
                    + "preserved) — set status to INACTIVE instead to remove it from the catalog. "
                    + "Any stray cart entries for this product are silently cleaned up; a cart isn't "
                    + "considered historical data the way an order is.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set stock to an absolute value",
            description = "Replaces the current stock count entirely — this is not an increment/decrement. "
                    + "For checkout, stock is decremented atomically elsewhere (Order); this endpoint is "
                    + "for manual admin correction (e.g. a fresh shipment arriving).")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id, @RequestParam Integer quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    // ---------------------------------------------------------
    // Product images
    // ---------------------------------------------------------

    @PostMapping("/{id}/images")
    public ResponseEntity<ProductImageResponse> addImage(
            @PathVariable Long id, @Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addImage(id, request));
    }

    @SecurityRequirements
    @GetMapping("/{id}/images")
    public ResponseEntity<List<ProductImageResponse>> getImages(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getImages(id));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark an image as the product's primary image",
            description = "Unsets any other image currently marked primary for this product first — "
                    + "at most one primary image per product is enforced here, not at the database level.")
    @PatchMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<ProductImageResponse> setPrimaryImage(
            @PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.setPrimaryImage(id, imageId));
    }
}
