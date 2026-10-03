package com.citymall.service;

import com.citymall.constant.CacheConstants;
import com.citymall.dto.ProductResponse;
import com.citymall.dto.admin.ProductStatusRequest;
import com.citymall.dto.admin.StockAdjustRequest;
import com.citymall.dto.admin.StockMovementResponse;
import com.citymall.entity.Product;
import com.citymall.entity.StockMovement;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.mapper.ProductMapper;
import com.citymall.repository.ProductRepository;
import com.citymall.repository.StockMovementRepository;
import com.citymall.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin product operations: filtered listing, enable/disable, stock
 * adjustment with movement log. Create/update/delete are reused from
 * {@link ProductService} (same validation + cache eviction).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final ProductMapper productMapper;

    /** Paginated + filterable (category/status/keyword). Keyword search covers all statuses. */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(Integer categoryId, Boolean active,
                                              String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Product> result;
        if (keyword != null && !keyword.isBlank()) {
            result = productRepository.searchForAdmin(keyword.trim(), categoryId, active, pageable);
        } else {
            result = productRepository.findForAdmin(categoryId, active, pageable);
        }
        Page<ProductResponse> mapped = result.map(productMapper::mapToProductResponse);
        return PageResponse.<ProductResponse>builder()
                .content(mapped.getContent())
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalElements(mapped.getTotalElements())
                .totalPages(mapped.getTotalPages())
                .first(mapped.isFirst())
                .last(mapped.isLast())
                .build();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_SEARCH_CACHE, allEntries = true)
    })
    @Transactional
    public ProductResponse setStatus(Integer id, ProductStatusRequest request) {
        Product product = findOrThrow(id);
        product.setActive(request.active());
        Product saved = productRepository.save(product);
        log.info("Admin set product id={} active={}", id, request.active());
        return productMapper.mapToProductResponse(saved);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_LIST_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CacheConstants.PRODUCT_SEARCH_CACHE, allEntries = true)
    })
    @Transactional
    public ProductResponse adjustStock(Integer id, StockAdjustRequest request) {
        if (request.quantityChange() == 0) {
            throw new BadRequestException("quantityChange cannot be zero");
        }
        Product product = findOrThrow(id);
        int current = product.getAvailableQuantity() != null ? product.getAvailableQuantity() : 0;
        int resulting = current + request.quantityChange();
        if (resulting < 0) {
            throw new BadRequestException("Adjustment would take stock negative for product id: "
                    + id + " (current=" + current + ", change=" + request.quantityChange() + ")");
        }
        product.setAvailableQuantity(resulting);
        Product saved = productRepository.save(product);
        movementRepository.save(StockMovement.builder()
                .product(saved)
                .quantityChange(request.quantityChange())
                .resultingQuantity(resulting)
                .reason(request.reason())
                .build());
        log.info("Admin stock adjust product id={} change={} resulting={}",
                id, request.quantityChange(), resulting);
        return productMapper.mapToProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> stockHistory(Integer id, int page, int size) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFound("Product not found with id: " + id);
        }
        Page<StockMovementResponse> result = movementRepository
                .findByProduct_IdOrderByCreatedAtDesc(id, PageRequest.of(page, size))
                .map(m -> new StockMovementResponse(
                        m.getId(), id, m.getQuantityChange(),
                        m.getResultingQuantity(), m.getReason(), m.getCreatedAt()));
        return PageResponse.<StockMovementResponse>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    private Product findOrThrow(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Product not found with id: " + id));
    }
}
