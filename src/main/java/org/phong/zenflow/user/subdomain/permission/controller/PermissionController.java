package org.phong.zenflow.user.subdomain.permission.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.user.subdomain.permission.dtos.CreatePermissionRequest;
import org.phong.zenflow.user.subdomain.permission.dtos.PermissionDto;
import org.phong.zenflow.user.subdomain.permission.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<PermissionDto> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        PermissionDto createdPermission = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPermission);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<PermissionDto>> createPermissions(@Valid @RequestBody List<CreatePermissionRequest> requests) {
        List<PermissionDto> createdPermissions = permissionService.createPermissions(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPermissions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable UUID id) {
        return permissionService.findById(id)
            .map(permission -> ResponseEntity.ok(permission))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        List<PermissionDto> permissions = permissionService.findAll();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<PermissionDto>> getAllPermissions(Pageable pageable) {
        Page<PermissionDto> permissions = permissionService.findAll(pageable);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/feature/{feature}")
    public ResponseEntity<List<PermissionDto>> getPermissionsByFeature(@PathVariable String feature) {
        List<PermissionDto> permissions = permissionService.findByFeature(feature);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<PermissionDto>> getPermissionsByAction(@PathVariable String action) {
        List<PermissionDto> permissions = permissionService.findByAction(action);
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/features")
    public ResponseEntity<List<PermissionDto>> getPermissionsByFeatures(@RequestBody List<String> features) {
        List<PermissionDto> permissions = permissionService.findByFeatures(features);
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/actions")
    public ResponseEntity<List<PermissionDto>> getPermissionsByActions(@RequestBody List<String> actions) {
        List<PermissionDto> permissions = permissionService.findByActions(actions);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/feature/{feature}/action/{action}")
    public ResponseEntity<PermissionDto> getPermissionByFeatureAndAction(
            @PathVariable String feature,
            @PathVariable String action) {
        return permissionService.findByFeatureAndAction(feature, action)
            .map(permission -> ResponseEntity.ok(permission))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check/feature/{feature}/action/{action}")
    public ResponseEntity<Boolean> checkPermissionExists(
            @PathVariable String feature,
            @PathVariable String action) {
        boolean exists = permissionService.existsByFeatureAndAction(feature, action);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/features")
    public ResponseEntity<List<String>> getAllFeatures() {
        List<String> features = permissionService.getAllFeatures();
        return ResponseEntity.ok(features);
    }

    @GetMapping("/actions")
    public ResponseEntity<List<String>> getAllActions() {
        List<String> actions = permissionService.getAllActions();
        return ResponseEntity.ok(actions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionDto> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePermissionRequest request) {
        PermissionDto updatedPermission = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(updatedPermission);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deletePermissions(@RequestBody List<UUID> ids) {
        permissionService.deletePermissions(ids);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getPermissionCount() {
        long count = permissionService.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> checkPermissionExists(@PathVariable UUID id) {
        boolean exists = permissionService.existsById(id);
        return ResponseEntity.ok(exists);
    }
}
