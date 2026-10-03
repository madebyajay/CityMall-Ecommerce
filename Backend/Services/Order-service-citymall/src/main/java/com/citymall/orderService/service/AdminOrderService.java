package com.citymall.orderService.service;

import com.citymall.exception.BadRequestException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.orderService.dto.OrderResponse;
import com.citymall.orderService.dto.OrderStatusRequest;
import com.citymall.orderService.entity.Order;
import com.citymall.orderService.entity.OrderStatus;
import com.citymall.orderService.mapper.OrderMapper;
import com.citymall.orderService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin order operations: filtered listing, detail, status updates.
 * Checkout-time creation stays in {@link OrderService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(String status, String customerId,
                                    java.time.LocalDateTime from,
                                    java.time.LocalDateTime to,
                                    Pageable pageable) {
        OrderStatus orderStatus = parseStatus(status);
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("'to' must not be before 'from'");
        }
        return repository.findForAdmin(orderStatus, customerId, from, to, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusRequest request) {
        Order order = findOrThrow(id);
        OrderStatus next = OrderStatus.valueOf(request.status().trim().toUpperCase());
        if (order.getStatus() == next) {
            return mapper.toResponse(order);
        }
        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot move order out of terminal status: "
                    + order.getStatus());
        }
        order.setStatus(next);
        Order saved = repository.save(order);
        log.info("Admin set order id={} status {} -> {}", id, order.getStatus(), next);
        return mapper.toResponse(saved);
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + status);
        }
    }

    private Order findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Order not found with id: " + id));
    }
}
