package com.ecommerce.service.impl;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.dto.product.ProductImageRequest;
import com.ecommerce.dto.product.ProductImageResponse;
import com.ecommerce.dto.product.ProductRequest;
import com.ecommerce.dto.product.ProductResponse;
import com.ecommerce.entity.Brand;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.SubCategory;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.InvalidRequestException;
import com.ecommerce.exception.ResourceInUseException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.repository.BrandRepository;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.ProductImageRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.SubCategoryRepository;
import com.ecommerce.repository.specification.ProductSpecification;
import com.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    // ---------------------------------------------------------
    // Product CRUD
    // ---------------------------------------------------------

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (request.getSku() != null && !request.getSku().isBlank()
                && productRepository.findBySku(request.getSku()).isPresent()) {
            throw new DuplicateResourceException("A product with SKU '" + request.getSku() + "' already exists");
        }

        Category category = findCategoryOrThrow(request.getCategoryId());
        SubCategory subCategory = resolveSubCategory(request.getSubCategoryId(), category);
        Brand brand = resolveBrand(request.getBrandId());

        Product product = Product.builder()
                .category(category)
                .subCategory(subCategory)
                .brand(brand)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .sku(request.getSku())
                .build();

        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return ProductMapper.toResponse(findProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(ProductFilterRequest filter, Pageable pageable) {
        Page<Product> page = productRepository.findAll(ProductSpecification.withFilters(filter), pageable);
        return PagedResponse.from(page.map(ProductMapper::toResponse));
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);

        if (request.getSku() != null && !request.getSku().isBlank()
                && !request.getSku().equalsIgnoreCase(product.getSku())) {
            productRepository.findBySku(request.getSku()).ifPresent(p -> {
                throw new DuplicateResourceException("A product with SKU '" + request.getSku() + "' already exists");
            });
        }

        Category category = findCategoryOrThrow(request.getCategoryId());
        SubCategory subCategory = resolveSubCategory(request.getSubCategoryId(), category);
        Brand brand = resolveBrand(request.getBrandId());

        product.setCategory(category);
        product.setSubCategory(subCategory);
        product.setBrand(brand);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setSku(request.getSku());

        return ProductMapper.toResponse(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);

        // Order history must never silently lose a line item — this is the
        // one reference that blocks deletion outright, not just a cleanup.
        if (orderItemRepository.existsByProductId(id)) {
            throw new ResourceInUseException(
                    "Product '" + product.getName() + "' has been ordered and cannot be deleted. "
                            + "Set its status to INACTIVE instead to remove it from the catalog "
                            + "while preserving order history.");
        }

        // Unlike order_items, a cart isn't a historical record — silently
        // dropping a discontinued product from active carts is fine and
        // expected, not data loss. product_images/reviews/wishlists all
        // cascade-delete at the DB level already (see V1 migration).
        cartItemRepository.deleteByProductId(id);

        productRepository.delete(product);
    }

    @Override
    public ProductResponse updateStock(Long id, Integer newStock) {
        if (newStock == null || newStock < 0) {
            throw new InvalidRequestException("Stock cannot be negative");
        }
        Product product = findProductOrThrow(id);
        product.setStock(newStock);
        return ProductMapper.toResponse(product);
    }

    // ---------------------------------------------------------
    // Product images
    // ---------------------------------------------------------

    @Override
    public ProductImageResponse addImage(Long productId, ProductImageRequest request) {
        Product product = findProductOrThrow(productId);

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            clearExistingPrimaryImage(productId);
        }

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(request.getImageUrl())
                .isPrimary(Boolean.TRUE.equals(request.getIsPrimary()))
                .build();

        return ProductMapper.toResponse(productImageRepository.save(image));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImages(Long productId) {
        findProductOrThrow(productId);
        return ProductMapper.toImageResponseList(productImageRepository.findByProductId(productId));
    }

    @Override
    public void deleteImage(Long productId, Long imageId) {
        findProductOrThrow(productId);
        ProductImage image = findImageOrThrow(imageId);
        assertImageBelongsToProduct(image, productId);
        productImageRepository.delete(image);
    }

    @Override
    public ProductImageResponse setPrimaryImage(Long productId, Long imageId) {
        findProductOrThrow(productId);
        ProductImage target = findImageOrThrow(imageId);
        assertImageBelongsToProduct(target, productId);

        clearExistingPrimaryImage(productId);
        target.setIsPrimary(true);

        return ProductMapper.toResponse(target);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private SubCategory resolveSubCategory(Long subCategoryId, Category category) {
        if (subCategoryId == null) {
            return null;
        }
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + subCategoryId));

        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new InvalidRequestException(
                    "Sub-category " + subCategoryId + " does not belong to category " + category.getId());
        }
        return subCategory;
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
    }

    private ProductImage findImageOrThrow(Long imageId) {
        return productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found with id: " + imageId));
    }

    private void assertImageBelongsToProduct(ProductImage image, Long productId) {
        if (!image.getProduct().getId().equals(productId)) {
            throw new InvalidRequestException(
                    "Image " + image.getId() + " does not belong to product " + productId);
        }
    }

    private void clearExistingPrimaryImage(Long productId) {
        productImageRepository.findByProductId(productId).stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .forEach(img -> img.setIsPrimary(false));
    }
}
