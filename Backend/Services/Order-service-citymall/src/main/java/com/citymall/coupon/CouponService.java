package com.citymall.coupon;

import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Admin coupon CRUD (checkout-time validation will consume this table next). */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository repository;
    private final CouponMapper mapper;

    public Page<CouponResponse> list(Boolean active, Pageable pageable) {
        Page<Coupon> page = active == null
                ? repository.findAll(pageable)
                : repository.findByActive(active, pageable);
        return page.map(mapper::toResponse);
    }

    public CouponResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = request.code().trim().toUpperCase();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Coupon already exists: " + code);
        }
        validate(request);
        Coupon saved = repository.save(Coupon.builder()
                .code(code)
                .description(request.description())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderAmount(request.minOrderAmount() != null
                        ? request.minOrderAmount() : BigDecimal.ZERO)
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .active(true)
                .build());
        log.info("Admin created coupon {}", code);
        return mapper.toResponse(saved);
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findOrThrow(id);
        String code = request.code().trim().toUpperCase();
        if (!code.equalsIgnoreCase(coupon.getCode())
                && repository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Coupon already exists: " + code);
        }
        validate(request);
        coupon.setCode(code);
        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinOrderAmount(request.minOrderAmount() != null
                ? request.minOrderAmount() : BigDecimal.ZERO);
        coupon.setValidFrom(request.validFrom());
        coupon.setValidTo(request.validTo());
        log.info("Admin updated coupon id={}", id);
        return mapper.toResponse(repository.save(coupon));
    }

    @Transactional
    public CouponResponse setStatus(Long id, CouponStatusRequest request) {
        Coupon coupon = findOrThrow(id);
        coupon.setActive(request.active());
        log.info("Admin set coupon id={} active={}", id, request.active());
        return mapper.toResponse(repository.save(coupon));
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = findOrThrow(id);
        log.info("Admin deleting coupon id={}", id);
        repository.delete(coupon);
    }

    private void validate(CouponRequest request) {
        if (!request.validTo().isAfter(request.validFrom())) {
            throw new BadRequestException("validTo must be after validFrom");
        }
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Percentage discount cannot exceed 100");
        }
        if (request.minOrderAmount() != null
                && request.minOrderAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("minOrderAmount cannot be negative");
        }
    }

    private Coupon findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Coupon not found with id: " + id));
    }
}
