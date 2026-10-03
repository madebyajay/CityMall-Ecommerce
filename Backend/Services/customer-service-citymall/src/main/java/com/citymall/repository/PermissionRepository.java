package com.citymall.repository;

import com.citymall.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByApiEndpoint(String apiEndpoint);

    List<Permission> findByActiveStatusTrue();
}
