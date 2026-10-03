package com.citymall.repository;

import com.citymall.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findAllByIdInOrderById(List<Integer> ids);
    @Query("""
    SELECT p
    FROM Product p
    JOIN p.category c
    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
""")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.id IN :ids
        ORDER BY p.id
       """)
    List<Product> findAllByIdInForUpdate(
            @Param("ids")
            List<Integer> ids
    );

    List<Product> findAllByIdIn(List<Integer> ids);

    // ---------------- Admin / storefront filtering ----------------
    Page<Product> findByActiveTrue(Pageable pageable);

    /** Storefront category browsing — hides admin-disabled products. */
    Page<Product> findByCategory_IdAndActiveTrue(Integer categoryId, Pageable pageable);

    Page<Product> findByCategory_Id(Integer categoryId, Pageable pageable);

    Page<Product> findByActive(Boolean active, Pageable pageable);

    /** Admin list: optional category + status filters in one query. */
    @Query("""
    SELECT p
    FROM Product p
    JOIN FETCH p.category c
    WHERE (:categoryId IS NULL OR c.id = :categoryId)
      AND (:active IS NULL OR p.active = :active)
    """)
    Page<Product> findForAdmin(
            @Param("categoryId") Integer categoryId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    /** Storefront search must hide disabled products. */
    @Query("""
    SELECT p
    FROM Product p
    JOIN p.category c
    WHERE p.active = true
      AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Product> searchActiveProducts(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /** Admin keyword search across all statuses + optional filters. */
    @Query("""
    SELECT p
    FROM Product p
    JOIN FETCH p.category c
    WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:categoryId IS NULL OR c.id = :categoryId)
      AND (:active IS NULL OR p.active = :active)
    """)
    Page<Product> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    long countByAvailableQuantityLessThan(int threshold);
}
