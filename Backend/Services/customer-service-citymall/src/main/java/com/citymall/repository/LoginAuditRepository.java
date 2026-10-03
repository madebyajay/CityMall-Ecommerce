package com.citymall.repository;

import com.citymall.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    /** Most recent attempts first — used to count consecutive failures since last success. */
    List<LoginAudit> findTop10ByUserIdOrderByActionDateDesc(Long userId);
}
