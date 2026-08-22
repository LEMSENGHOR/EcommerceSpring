package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.dto.product.ProductImageRequest;
import com.ecommerce.dto.product.ProductImageResponse;
import com.ecommerce.dto.product.ProductRequest;
import com.ecommerce.dto.product.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(Long id);

    PagedResponse<ProductResponse> searchProducts(ProductFilterRequest filter, Pageable pageable);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    ProductResponse updateStock(Long id, Integer newStock);

    ProductImageResponse addImage(Long productId, ProductImageRequest request);

    List<ProductImageResponse> getImages(Long productId);

    void deleteImage(Long productId, Long imageId);

    ProductImageResponse setPrimaryImage(Long productId, Long imageId);
}
