package org.phong.zenflow.user.subdomain.permission.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.user.subdomain.permission.dtos.CreatePermissionRequest;
import org.phong.zenflow.user.subdomain.permission.dtos.PermissionDto;
import org.phong.zenflow.user.subdomain.permission.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<RestApiResponse<PermissionDto>> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        PermissionDto createdPermission = permissionService.createPermission(request);
        return RestApiResponse.created(createdPermission, "Permission created successfully");
    }

    @PostMapping("/bulk")
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> createPermissions(@Valid @RequestBody List<CreatePermissionRequest> requests) {
        List<PermissionDto> createdPermissions = permissionService.createPermissions(requests);
        return RestApiResponse.created(createdPermissions, "Permissions created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<PermissionDto>> getPermissionById(@PathVariable UUID id) {
        PermissionDto permission = permissionService.findById(id);
        return RestApiResponse.success(permission, "Permission retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> getAllPermissions() {
        List<PermissionDto> permissions = permissionService.findAll();
        return RestApiResponse.success(permissions, "Permissions retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> getAllPermissions(Pageable pageable) {
        Page<PermissionDto> permissions = permissionService.findAll(pageable);
        return RestApiResponse.success(permissions, "Permissions retrieved successfully");
    }

    @GetMapping("/feature/{feature}")
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> getPermissionsByFeature(@PathVariable String feature) {
        List<PermissionDto> permissions = permissionService.findByFeature(feature);
        return RestApiResponse.success(permissions, "Permissions retrieved successfully");
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> getPermissionsByAction(@PathVariable String action) {
        List<PermissionDto> permissions = permissionService.findByAction(action);
        return RestApiResponse.success(permissions, "Permissions retrieved successfully");
    }

    @PostMapping("/features")
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> getPermissionsByFeatures(@RequestBody List<String> features) {
        List<PermissionDto> permissions = permissionService.findByFeatures(features);
        return RestApiResponse.success(permissions, "Permissions retrieved successfully");
    }

    @PostMapping("/actions")
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> getPermissionsByActions(@RequestBody List<String> actions) {
        List<PermissionDto> permissions = permissionService.findByActions(actions);
        return RestApiResponse.success(permissions, "Permissions retrieved successfully");
    }

    @GetMapping("/feature/{feature}/action/{action}")
    public ResponseEntity<RestApiResponse<PermissionDto>> getPermissionByFeatureAndAction(
            @PathVariable String feature,
            @PathVariable String action) {
        PermissionDto permission = permissionService.findByFeatureAndAction(feature, action);
        return RestApiResponse.success(permission, "Permission retrieved successfully");
    }

    @GetMapping("/check/feature/{feature}/action/{action}")
    public ResponseEntity<RestApiResponse<Boolean>> checkPermissionExists(
            @PathVariable String feature,
            @PathVariable String action) {
        boolean exists = permissionService.existsByFeatureAndAction(feature, action);
        return RestApiResponse.success(exists, "Permission existence checked");
    }

    @GetMapping("/features")
    public ResponseEntity<RestApiResponse<List<String>>> getAllFeatures() {
        List<String> features = permissionService.getAllFeatures();
        return RestApiResponse.success(features, "All features retrieved successfully");
    }

    @GetMapping("/actions")
    public ResponseEntity<RestApiResponse<List<String>>> getAllActions() {
        List<String> actions = permissionService.getAllActions();
        return RestApiResponse.success(actions, "All actions retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<PermissionDto>> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePermissionRequest request) {
        PermissionDto updatedPermission = permissionService.updatePermission(id, request);
        return RestApiResponse.success(updatedPermission, "Permission updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deletePermission(@PathVariable UUID id) {
        permissionService.deletePermission(id);
        return RestApiResponse.success("Permission deleted successfully");
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<RestApiResponse<Void>> deletePermissions(@RequestBody List<UUID> ids) {
        permissionService.deletePermissions(ids);
        return RestApiResponse.success("Permissions deleted successfully");
    }

    @GetMapping("/count")
    public ResponseEntity<RestApiResponse<Long>> getPermissionCount() {
        long count = permissionService.count();
        return RestApiResponse.success(count, "Permission count retrieved successfully");
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkPermissionExists(@PathVariable UUID id) {
        boolean exists = permissionService.existsById(id);
        return RestApiResponse.success(exists, "Permission existence checked");
    }
}
