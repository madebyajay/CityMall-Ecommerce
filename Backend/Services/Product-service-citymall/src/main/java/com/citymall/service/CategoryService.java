package com.citymall.service;

import com.citymall.dto.CategoryRequest;
import com.citymall.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

//    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse findCategoryById(Integer id);
    List<CategoryResponse> findAllCategories();
//    CategoryResponse updateCategory(Integer id, CategoryRequest request);
//    void deleteCategory(Integer id);
}
