package com.citymall.service;

import com.citymall.dto.role.RoleRequest;
import com.citymall.dto.role.RoleResponse;
import com.citymall.entity.Role;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.mapper.UserMapper;
import com.citymall.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserMapper mapper;

    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream().map(mapper::toRoleResponse).toList();
    }

    public RoleResponse getById(Long id) {
        return mapper.toRoleResponse(findOrThrow(id));
    }

    @Transactional
    public RoleResponse create(RoleRequest req) {
        String name = req.name().trim().toUpperCase();
        if (roleRepository.existsByName(name)) {
            throw new BusinessException("Role already exists: " + name);
        }
        Role saved = roleRepository.save(Role.builder()
                .name(name)
                .description(req.description())
                .activeStatus(true)
                .build());
        log.info("Created role {}", name);
        return mapper.toRoleResponse(saved);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest req) {
        Role role = findOrThrow(id);
        assertNotSystemRole(role, "renamed");
        if (req.name() != null && !req.name().isBlank()) {
            String name = req.name().trim().toUpperCase();
            if (!name.equals(role.getName()) && roleRepository.existsByName(name)) {
                throw new BusinessException("Role already exists: " + name);
            }
            role.setName(name);
        }
        if (req.description() != null) role.setDescription(req.description());
        return mapper.toRoleResponse(roleRepository.save(role));
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        Role role = findOrThrow(id);
        if (!active) {
            assertNotSystemRole(role, "deactivated");
            if (roleRepository.count() <= 1) {
                throw new BadRequestException("Cannot deactivate the last role");
            }
        }
        role.setActiveStatus(active);
        roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = findOrThrow(id);
        assertNotSystemRole(role, "deleted");
        roleRepository.delete(role);
    }

    private static void assertNotSystemRole(Role role, String action) {
        if (List.of("SUPERADMIN", "ADMIN", "CUSTOMER", "SELLER").contains(role.getName())) {
            throw new BadRequestException("System role cannot be " + action + ": " + role.getName());
        }
    }

    private Role findOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("Role not found with id: " + id));
    }
}
