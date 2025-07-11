package org.phong.zenflow.user.subdomain.role.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.user.subdomain.role.dtos.CreateRoleRequest;
import org.phong.zenflow.user.subdomain.role.dtos.RoleDto;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.phong.zenflow.user.subdomain.role.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RestApiResponse<RoleDto>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleDto createdRole = roleService.createRole(request);
        return RestApiResponse.created(createdRole, "Role created successfully");
    }

    @PostMapping("/bulk")
    public ResponseEntity<RestApiResponse<List<RoleDto>>> createRoles(@Valid @RequestBody List<CreateRoleRequest> requests) {
        List<RoleDto> createdRoles = roleService.createRoles(requests);
        return RestApiResponse.created(createdRoles, "Roles created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<RoleDto>> getRoleById(@PathVariable UUID id) {
        RoleDto role = roleService.findById(id);
        return RestApiResponse.success(role, "Role retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<RoleDto>>> getAllRoles() {
        List<RoleDto> roles = roleService.findAll();
        return RestApiResponse.success(roles, "Roles retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<RoleDto>>> getAllRoles(Pageable pageable) {
        Page<RoleDto> roles = roleService.findAll(pageable);
        return RestApiResponse.success(roles, "Roles retrieved successfully");
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RestApiResponse<RoleDto>> getRoleByName(@PathVariable UserRoleEnum name) {
        RoleDto role = roleService.findByName(name);
        return RestApiResponse.success(role, "Role retrieved successfully");
    }

    @PostMapping("/names")
    public ResponseEntity<RestApiResponse<List<RoleDto>>> getRolesByNames(@RequestBody List<UserRoleEnum> names) {
        List<RoleDto> roles = roleService.findByNames(names);
        return RestApiResponse.success(roles, "Roles retrieved successfully");
    }

    @GetMapping("/check/name/{name}")
    public ResponseEntity<RestApiResponse<Boolean>> checkRoleExistsByName(@PathVariable UserRoleEnum name) {
        boolean exists = roleService.existsByName(name);
        return RestApiResponse.success(exists, "Role existence checked");
    }

    @GetMapping("/default")
    public ResponseEntity<RestApiResponse<RoleDto>> getDefaultRole() {
        RoleDto role = roleService.getDefaultRole();
        return RestApiResponse.success(role, "Default role retrieved successfully");
    }

    @GetMapping("/admin")
    public ResponseEntity<RestApiResponse<RoleDto>> getAdminRole() {
        RoleDto role = roleService.getAdminRole();
        return RestApiResponse.success(role, "Admin role retrieved successfully");
    }

    @GetMapping("/owner")
    public ResponseEntity<RestApiResponse<RoleDto>> getOwnerRole() {
        RoleDto role = roleService.getOwnerRole();
        return RestApiResponse.success(role, "Owner role retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<RoleDto>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRoleRequest request) {
        RoleDto updatedRole = roleService.updateRole(id, request);
        return RestApiResponse.success(updatedRole, "Role updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return RestApiResponse.success("Role deleted successfully");
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<RestApiResponse<Void>> deleteRoles(@RequestBody List<UUID> ids) {
        roleService.deleteRoles(ids);
        return RestApiResponse.success("Roles deleted successfully");
    }

    @GetMapping("/count")
    public ResponseEntity<RestApiResponse<Long>> getRoleCount() {
        long count = roleService.count();
        return RestApiResponse.success(count, "Role count retrieved successfully");
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkRoleExists(@PathVariable UUID id) {
        boolean exists = roleService.existsById(id);
        return RestApiResponse.success(exists, "Role existence checked");
    }
}
