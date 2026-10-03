package com.citymall.service;

import com.citymall.dto.CategoryRequest;
import com.citymall.dto.CategoryResponse;
import com.citymall.entity.Category;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.mapper.CategoryMapper;
import com.citymall.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class CategoryServiceImp implements CategoryService{

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

//    @Override
//    public CategoryResponse createCategory(CategoryRequest request) {
//
//        if(repository.existsByNameIgnoreCase(request.name())) {
//            throw new BusinessException("Category already exists");
//        }
//
//        Category category = mapper.toEntity(request);
//        log.info("Creating category: {}", request.name());
//        return mapper.toResponse(repository.save(category));
//    }

    @Override
    public CategoryResponse findCategoryById(Integer id) {
        Category category = repository.findById(id).orElseThrow(() -> new ResourceNotFound("Category not found with id " + id));
        return mapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> findAllCategories() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

//    @Override
//    @Transactional
//    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
//        Category category = repository.findById(id).orElseThrow(() -> new ResourceNotFound("Category not found"));
//
//        category.setName(request.name());
//        category.setDescription(request.description());
//        return mapper.toResponse(repository.save(category));
//    }

//    @Override
//    @Transactional
//    public void deleteCategory(Integer id) {
//        Category category = repository.findById(id).orElseThrow(() -> new ResourceNotFound("Category not found"));
//        if(!category.getProducts().isEmpty()) {
//            throw new BusinessException("Cannot delete category. Products exist under it.");
//        }
//        log.info("Deleting category id: {}", id);
//        repository.delete(category);
//    }
}
