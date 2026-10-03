package com.citymall.orderService.repository;

import com.citymall.orderService.entity.Order;
import com.citymall.orderService.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(String customerId);
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);

    /** Admin list: optional status / customer / date-range filters in one query. */
    @Query("""
    SELECT o
    FROM Order o
    WHERE (:status IS NULL OR o.status = :status)
      AND (:customerId IS NULL OR o.customerId = :customerId)
      AND (CAST(:from AS timestamp) IS NULL OR o.createdDate >= :from)
      AND (CAST(:to AS timestamp) IS NULL OR o.createdDate <= :to)
    """)
    Page<Order> findForAdmin(
            @Param("status") OrderStatus status,
            @Param("customerId") String customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
