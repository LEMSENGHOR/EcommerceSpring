package com.ecommerce.service;

import com.ecommerce.dto.category.CategoryRequest;
import com.ecommerce.dto.category.CategoryResponse;
import com.ecommerce.dto.category.SubCategoryRequest;
import com.ecommerce.dto.category.SubCategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getAllCategories();

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    SubCategoryResponse createSubCategory(SubCategoryRequest request);

    SubCategoryResponse getSubCategoryById(Long id);

    List<SubCategoryResponse> getSubCategoriesByCategory(Long categoryId);

    SubCategoryResponse updateSubCategory(Long id, SubCategoryRequest request);

    void deleteSubCategory(Long id);
}
