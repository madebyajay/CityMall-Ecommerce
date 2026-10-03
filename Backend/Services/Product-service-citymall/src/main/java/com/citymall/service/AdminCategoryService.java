package com.citymall.service;

import com.citymall.constant.CacheConstants;
import com.citymall.dto.admin.AdminCategoryRequest;
import com.citymall.dto.admin.AdminCategoryResponse;
import com.citymall.entity.Category;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin category management (top-level + subcategories via {@code parentId}).
 * Product caches are evicted on mutation because {@code ProductResponse}
 * embeds the category name.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository repository;

    public List<AdminCategoryResponse> list(Integer parentId) {
        List<Category> all = repository.findAll();
        return all.stream()
                .filter(c -> parentId == null
                        ? c.getParent() == null
                        : c.getParent() != null && parentId.equals(c.getParent().getId()))
                .map(this::toResponse)
                .toList();
    }

    public AdminCategoryResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_SEARCH_CACHE, allEntries = true)
    })
    @Transactional
    public AdminCategoryResponse create(AdminCategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessException("Category already exists: " + request.name());
        }
        Category category = Category.builder()
                .name(request.name().trim())
                .description(request.description())
                .parent(resolveParent(request.parentId()))
                .build();
        log.info("Admin creating category '{}'", request.name());
        return toResponse(repository.save(category));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_SEARCH_CACHE, allEntries = true)
    })
    @Transactional
    public AdminCategoryResponse update(Integer id, AdminCategoryRequest request) {
        Category category = findOrThrow(id);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(category.getName())
                && repository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Category already exists: " + name);
        }
        Category parent = resolveParent(request.parentId());
        if (parent != null && parent.getId().equals(id)) {
            throw new BadRequestException("Category cannot be its own parent");
        }
        category.setName(name);
        category.setDescription(request.description());
        category.setParent(parent);
        log.info("Admin updating category id={}", id);
        return toResponse(repository.save(category));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_SEARCH_CACHE, allEntries = true)
    })
    @Transactional
    public void delete(Integer id) {
        Category category = findOrThrow(id);
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new BusinessException("Cannot delete category. Products exist under it.");
        }
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new BusinessException("Cannot delete category. Subcategories exist under it.");
        }
        log.info("Admin deleting category id={}", id);
        repository.delete(category);
    }

    private Category resolveParent(Integer parentId) {
        if (parentId == null) return null;
        Category parent = findOrThrow(parentId);
        if (parent.getParent() != null) {
            throw new BadRequestException("Subcategory cannot be a parent (max depth: 2)");
        }
        return parent;
    }

    private Category findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Category not found with id: " + id));
    }

    private AdminCategoryResponse toResponse(Category c) {
        return new AdminCategoryResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getParent() != null ? c.getParent().getName() : null);
    }
}
