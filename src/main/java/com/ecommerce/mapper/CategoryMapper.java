package com.ecommerce.mapper;

import com.ecommerce.dto.category.CategoryResponse;
import com.ecommerce.dto.category.SubCategoryResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.SubCategory;

import java.util.List;

public class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus() != null ? category.getStatus().name() : null)
                .createdAt(category.getCreatedAt())
                .subCategories(toSubCategoryResponseList(category.getSubCategories() != null
                        ? category.getSubCategories().stream().toList()
                        : List.of()))
                .build();
    }

    public static SubCategoryResponse toResponse(SubCategory subCategory) {
        return SubCategoryResponse.builder()
                .id(subCategory.getId())
                .categoryId(subCategory.getCategory() != null ? subCategory.getCategory().getId() : null)
                .name(subCategory.getName())
                .description(subCategory.getDescription())
                .status(subCategory.getStatus() != null ? subCategory.getStatus().name() : null)
                .createdAt(subCategory.getCreatedAt())
                .build();
    }

    public static List<CategoryResponse> toCategoryResponseList(List<Category> categories) {
        return categories.stream().map(CategoryMapper::toResponse).toList();
    }

    public static List<SubCategoryResponse> toSubCategoryResponseList(List<SubCategory> subCategories) {
        return subCategories.stream().map(CategoryMapper::toResponse).toList();
    }
}
