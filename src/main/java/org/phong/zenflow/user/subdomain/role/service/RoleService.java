package org.phong.zenflow.user.subdomain.role.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.user.subdomain.role.dtos.CreateRoleRequest;
import org.phong.zenflow.user.subdomain.role.dtos.RoleDto;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.phong.zenflow.user.subdomain.role.infrastructure.mapstruct.RoleMapper;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.Role;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.repositories.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /**
     * Create a new role using DTO
     */
    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        Role role = roleMapper.toEntity(request);
        Role savedRole = roleRepository.save(role);
        return roleMapper.toDto(savedRole);
    }

    /**
     * Create multiple roles in bulk
     */
    @Transactional
    public List<RoleDto> createRoles(List<CreateRoleRequest> requests) {
        List<Role> roles = requests.stream()
            .map(roleMapper::toEntity)
            .toList();

        List<Role> savedRoles = roleRepository.saveAll(roles);
        return roleMapper.toDtoList(savedRoles);
    }

    /**
     * Find a role by ID
     */
    public RoleDto findById(UUID id) {
        return roleRepository.findById(id)
            .map(roleMapper::toDto)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
    }

    /**
     * Find all roles
     */
    public List<RoleDto> findAll() {
        List<Role> roles = roleRepository.findAll();
        return roleMapper.toDtoList(roles);
    }

    /**
     * Find roles with pagination
     */
    public Page<RoleDto> findAll(Pageable pageable) {
        return roleRepository.findAll(pageable)
            .map(roleMapper::toDto);
    }

    /**
     * Update a role using DTO
     */
    @Transactional
    public RoleDto updateRole(UUID id, CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));

        roleMapper.updateEntity(request, role);
        Role savedRole = roleRepository.save(role);
        return roleMapper.toDto(savedRole);
    }

    /**
     * Delete role
     */
    @Transactional
    public void deleteRole(UUID id) {
        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }

    /**
     * Delete multiple roles in bulk
     */
    @Transactional
    public void deleteRoles(List<UUID> ids) {
        List<Role> roles = roleRepository.findByIdIn(ids);
        roleRepository.deleteAll(roles);
    }

    /**
     * Check if a role exists
     */
    public boolean existsById(UUID id) {
        return roleRepository.existsById(id);
    }

    /**
     * Count all roles
     */
    public long count() {
        return roleRepository.count();
    }

    /**
     * Find a role by name using repository method
     */
    public RoleDto findByName(UserRoleEnum name) {
        return roleRepository.findByName(name)
            .map(roleMapper::toDto)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with name: " + name));
    }

    /**
     * Find roles by multiple names in bulk
     */
    public List<RoleDto> findByNames(List<UserRoleEnum> names) {
        List<Role> roles = roleRepository.findByNameIn(names);
        return roleMapper.toDtoList(roles);
    }

    /**
     * Check if a role exists by name using repository method
     */
    public boolean existsByName(UserRoleEnum name) {
        return roleRepository.existsByName(name);
    }

    /**
     * Get default user role
     */
    public RoleDto getDefaultRole() {
        return findByName(UserRoleEnum.USER);
    }

    /**
     * Get admin role
     */
    public RoleDto getAdminRole() {
        return findByName(UserRoleEnum.ADMIN);
    }

    /**
     * Get an owner role
     */
    public RoleDto getOwnerRole() {
        return findByName(UserRoleEnum.OWNER);
    }

    // Internal method for getting entity (used by other services)
    public Optional<Role> findEntityByName(UserRoleEnum name) {
        return roleRepository.findByName(name);
    }
}
