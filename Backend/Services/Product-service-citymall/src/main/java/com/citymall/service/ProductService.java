package com.citymall.service;

import com.citymall.dto.ProductRequest;
import com.citymall.dto.ProductResponse;
import com.citymall.dto.PurchaseRequest;
import com.citymall.dto.PurchaseResponse;
import com.citymall.response.PageResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    /** Raw lookup (admin + internal use) — returns inactive products too. */
    ProductResponse findProductById(Integer id);
    /** Storefront detail — 404s for admin-disabled products. */
    ProductResponse findActiveProductById(Integer id);
    /** Storefront listing — active only, optional category filter. */
    PageResponse<ProductResponse> findActiveProducts(Integer categoryId, int page, int size);
    void deleteProductById(Integer id);
    ProductResponse updateProduct(Integer id, ProductRequest request);
    List<PurchaseResponse> purchaseProducts(List<PurchaseRequest> request);
    PageResponse<ProductResponse> searchProducts(String keyword, int page, int size);
    List<PurchaseResponse> validateProducts(List<PurchaseRequest> requests);
}
