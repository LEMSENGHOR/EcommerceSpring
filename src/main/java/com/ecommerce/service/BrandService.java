package com.ecommerce.service;

import com.ecommerce.dto.brand.BrandRequest;
import com.ecommerce.dto.brand.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse createBrand(BrandRequest request);

    BrandResponse getBrandById(Long id);

    List<BrandResponse> getAllBrands();

    BrandResponse updateBrand(Long id, BrandRequest request);

    void deleteBrand(Long id);
}
