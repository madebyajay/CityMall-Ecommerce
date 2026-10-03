package com.citymall.repository;

import com.citymall.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    Optional<RolePermission> findByRole_NameAndPermission_ApiEndpoint(String roleName, String apiEndpoint);

    List<RolePermission> findByRole_Name(String roleName);

    @Query("""
            select rp.permission.apiEndpoint from RolePermission rp
            where rp.role.name = :roleName and rp.activeStatus = true
              and rp.permission.activeStatus = true
            """)
    List<String> findActiveEndpointsByRole(@Param("roleName") String roleName);

    void deleteByRole_NameAndPermission_ApiEndpoint(String roleName, String apiEndpoint);
}
