package org.phong.zenflow.user.subdomain.permission.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.user.subdomain.permission.dtos.CreatePermissionRequest;
import org.phong.zenflow.user.subdomain.permission.dtos.PermissionDto;
import org.phong.zenflow.user.subdomain.permission.exception.PermissionNotFoundException;
import org.phong.zenflow.user.subdomain.permission.infrastructure.mapstruct.PermissionMapper;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.entities.Permission;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.projections.PermissionActionProjection;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.projections.PermissionFeatureProjection;
import org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.repositories.PermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    /**
     * Create a new permission using DTO
     */
    @Transactional
    @AuditLog(action = AuditAction.PERMISSION_CREATE, targetIdExpression = "returnObject.id")
    public PermissionDto createPermission(CreatePermissionRequest request) {
        Permission permission = permissionMapper.toEntity(request);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(savedPermission);
    }

    /**
     * Create multiple permissions in bulk
     */
    @Transactional
    @AuditLog(action = AuditAction.PERMISSION_CREATE, description = "Bulk permission creation", targetIdExpression = "returnObject.![id]")
    public List<PermissionDto> createPermissions(List<CreatePermissionRequest> requests) {
        List<Permission> permissions = requests.stream()
            .map(permissionMapper::toEntity)
            .toList();

        List<Permission> savedPermissions = permissionRepository.saveAll(permissions);
        return permissionMapper.toDtoList(savedPermissions);
    }

    /**
     * Find permission by ID
     */
    public PermissionDto findById(UUID id) {
        return permissionRepository.findById(id)
            .map(permissionMapper::toDto)
            .orElseThrow(() -> new PermissionNotFoundException(id.toString()));
    }

    /**
     * Find all permissions
     */
    public List<PermissionDto> findAll() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissionMapper.toDtoList(permissions);
    }

    /**
     * Find permissions with pagination
     */
    public Page<PermissionDto> findAll(Pageable pageable) {
        return permissionRepository.findAll(pageable)
            .map(permissionMapper::toDto);
    }

    /**
     * Update permission using DTO
     */
    @Transactional
    @AuditLog(action = AuditAction.PERMISSION_UPDATE, targetIdExpression = "#id")
    public PermissionDto updatePermission(UUID id, CreatePermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new PermissionNotFoundException(id.toString()));

        permissionMapper.updateEntity(request, permission);
        Permission savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(savedPermission);
    }

    /**
     * Delete permission
     */
    @Transactional
    @AuditLog(action = AuditAction.PERMISSION_DELETE, targetIdExpression = "#id")
    public void deletePermission(UUID id) {
        if (!permissionRepository.existsById(id)) {
            throw new PermissionNotFoundException(id.toString());
        }
        permissionRepository.deleteById(id);
    }

    /**
     * Delete multiple permissions in bulk
     */
    @Transactional
    @AuditLog(action = AuditAction.PERMISSION_DELETE, description = "Bulk permission deletion", targetIdExpression = "#ids")
    public void deletePermissions(List<UUID> ids) {
        List<Permission> permissions = permissionRepository.findByIdIn(ids);
        permissionRepository.deleteAll(permissions);
    }

    /**
     * Check if permission exists
     */
    public boolean existsById(UUID id) {
        return permissionRepository.existsById(id);
    }

    /**
     * Count all permissions
     */
    public long count() {
        return permissionRepository.count();
    }

    /**
     * Find permissions by feature using repository method
     */
    public List<PermissionDto> findByFeature(String feature) {
        List<Permission> permissions = permissionRepository.findByFeature(feature);
        return permissionMapper.toDtoList(permissions);
    }

    /**
     * Find permissions by action using repository method
     */
    public List<PermissionDto> findByAction(String action) {
        List<Permission> permissions = permissionRepository.findByAction(action);
        return permissionMapper.toDtoList(permissions);
    }

    /**
     * Find permissions by multiple features in bulk
     */
    public List<PermissionDto> findByFeatures(List<String> features) {
        List<Permission> permissions = permissionRepository.findByFeatureIn(features);
        return permissionMapper.toDtoList(permissions);
    }

    /**
     * Find permissions by multiple actions in bulk
     */
    public List<PermissionDto> findByActions(List<String> actions) {
        List<Permission> permissions = permissionRepository.findByActionIn(actions);
        return permissionMapper.toDtoList(permissions);
    }

    /**
     * Find permission by feature and action using repository method
     */
    public PermissionDto findByFeatureAndAction(String feature, String action) {
        return permissionRepository.findByFeatureAndAction(feature, action)
            .map(permissionMapper::toDto)
            .orElseThrow(() -> new PermissionNotFoundException(feature, action));
    }

    /**
     * Check if permission exists by feature and action using repository method
     */
    public boolean existsByFeatureAndAction(String feature, String action) {
        return permissionRepository.existsByFeatureAndAction(feature, action);
    }

    /**
     * Get all unique features using projection for efficiency
     */
    public List<String> getAllFeatures() {
        return permissionRepository.findAllFeatures().stream()
            .map(PermissionFeatureProjection::getFeature)
            .toList();
    }

    /**
     * Get all unique actions using projection for efficiency
     */
    public List<String> getAllActions() {
        return permissionRepository.findAllActions().stream()
            .map(PermissionActionProjection::getAction)
            .toList();
    }
}
