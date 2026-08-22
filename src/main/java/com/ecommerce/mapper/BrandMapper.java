package com.ecommerce.mapper;

import com.ecommerce.dto.brand.BrandResponse;
import com.ecommerce.entity.Brand;

import java.util.List;

public class BrandMapper {

    private BrandMapper() {
    }

    public static BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .logo(brand.getLogo())
                .status(brand.getStatus() != null ? brand.getStatus().name() : null)
                .createdAt(brand.getCreatedAt())
                .build();
    }

    public static List<BrandResponse> toResponseList(List<Brand> brands) {
        return brands.stream().map(BrandMapper::toResponse).toList();
    }
}
