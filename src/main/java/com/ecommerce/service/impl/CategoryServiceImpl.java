package com.ecommerce.service.impl;

import com.ecommerce.dto.category.CategoryRequest;
import com.ecommerce.dto.category.CategoryResponse;
import com.ecommerce.dto.category.SubCategoryRequest;
import com.ecommerce.dto.category.SubCategoryResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.SubCategory;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceInUseException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.CategoryMapper;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.SubCategoryRepository;
import com.ecommerce.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;

    // ---------------------------------------------------------
    // Category
    // ---------------------------------------------------------

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "A category named '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = findCategoryOrThrow(id);
        return CategoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return CategoryMapper.toCategoryResponseList(categoryRepository.findAll());
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        // Only enforce uniqueness if the name is actually changing.
        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "A category named '" + request.getName() + "' already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return CategoryMapper.toResponse(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = findCategoryOrThrow(id);

        // Products referencing this category have no cascade/set-null at the
        // DB level (fk_product_category) — pre-check and fail cleanly (409)
        // rather than letting a raw FK violation surface as a 500. Resolves
        // the gap flagged since Phase 3.
        if (productRepository.existsByCategoryId(id)) {
            throw new ResourceInUseException(
                    "Category '" + category.getName() + "' has products and cannot be deleted");
        }

        // Sub-categories still cascade-delete via the entity mapping (orphanRemoval = true).
        categoryRepository.delete(category);
    }

    // ---------------------------------------------------------
    // SubCategory
    // ---------------------------------------------------------

    @Override
    public SubCategoryResponse createSubCategory(SubCategoryRequest request) {
        Category category = findCategoryOrThrow(request.getCategoryId());

        SubCategory subCategory = SubCategory.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        SubCategory saved = subCategoryRepository.save(subCategory);
        return CategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubCategoryResponse getSubCategoryById(Long id) {
        return CategoryMapper.toResponse(findSubCategoryOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoryResponse> getSubCategoriesByCategory(Long categoryId) {
        // Ensure the parent category actually exists before listing children.
        findCategoryOrThrow(categoryId);
        return CategoryMapper.toSubCategoryResponseList(
                subCategoryRepository.findByCategoryId(categoryId));
    }

    @Override
    public SubCategoryResponse updateSubCategory(Long id, SubCategoryRequest request) {
        SubCategory subCategory = findSubCategoryOrThrow(id);

        if (!subCategory.getCategory().getId().equals(request.getCategoryId())) {
            Category newParent = findCategoryOrThrow(request.getCategoryId());
            subCategory.setCategory(newParent);
        }

        subCategory.setName(request.getName());
        subCategory.setDescription(request.getDescription());

        return CategoryMapper.toResponse(subCategory);
    }

    @Override
    public void deleteSubCategory(Long id) {
        SubCategory subCategory = findSubCategoryOrThrow(id);
        subCategoryRepository.delete(subCategory);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private SubCategory findSubCategoryOrThrow(Long id) {
        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with id: " + id));
    }
}
