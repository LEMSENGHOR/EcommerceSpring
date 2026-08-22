package com.ecommerce.service.impl;

import com.ecommerce.dto.brand.BrandRequest;
import com.ecommerce.dto.brand.BrandResponse;
import com.ecommerce.entity.Brand;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceInUseException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.BrandMapper;
import com.ecommerce.repository.BrandRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    @Override
    public BrandResponse createBrand(BrandRequest request) {
        brandRepository.findByName(request.getName()).ifPresent(b -> {
            throw new DuplicateResourceException(
                    "A brand named '" + request.getName() + "' already exists");
        });

        Brand brand = Brand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .logo(request.getLogo())
                .build();

        return BrandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        return BrandMapper.toResponse(findBrandOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return BrandMapper.toResponseList(brandRepository.findAll());
    }

    @Override
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = findBrandOrThrow(id);

        if (!brand.getName().equalsIgnoreCase(request.getName())) {
            brandRepository.findByName(request.getName()).ifPresent(b -> {
                throw new DuplicateResourceException(
                        "A brand named '" + request.getName() + "' already exists");
            });
        }

        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogo(request.getLogo());

        return BrandMapper.toResponse(brand);
    }

    @Override
    public void deleteBrand(Long id) {
        Brand brand = findBrandOrThrow(id);

        // fk_product_brand has no ON DELETE CASCADE/SET NULL — pre-check and
        // fail cleanly (409) rather than a raw FK violation. Resolves the gap
        // flagged since Phase 3, same fix as CategoryServiceImpl.deleteCategory.
        if (productRepository.existsByBrandId(id)) {
            throw new ResourceInUseException(
                    "Brand '" + brand.getName() + "' has products and cannot be deleted");
        }

        brandRepository.delete(brand);
    }

    private Brand findBrandOrThrow(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }
}
