package org.phong.zenflow.user.subdomain.role.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.user.subdomain.role.dtos.CreateRoleRequest;
import org.phong.zenflow.user.subdomain.role.dtos.RoleDto;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.phong.zenflow.user.subdomain.role.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleDto createdRole = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<RoleDto>> createRoles(@Valid @RequestBody List<CreateRoleRequest> requests) {
        List<RoleDto> createdRoles = roleService.createRoles(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRoles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable UUID id) {
        return roleService.findById(id)
            .map(role -> ResponseEntity.ok(role))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<RoleDto> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<RoleDto>> getAllRoles(Pageable pageable) {
        Page<RoleDto> roles = roleService.findAll(pageable);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable UserRoleEnum name) {
        return roleService.findByName(name)
            .map(role -> ResponseEntity.ok(role))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/names")
    public ResponseEntity<List<RoleDto>> getRolesByNames(@RequestBody List<UserRoleEnum> names) {
        List<RoleDto> roles = roleService.findByNames(names);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/check/name/{name}")
    public ResponseEntity<Boolean> checkRoleExistsByName(@PathVariable UserRoleEnum name) {
        boolean exists = roleService.existsByName(name);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/default")
    public ResponseEntity<RoleDto> getDefaultRole() {
        return roleService.getDefaultRole()
            .map(role -> ResponseEntity.ok(role))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin")
    public ResponseEntity<RoleDto> getAdminRole() {
        return roleService.getAdminRole()
            .map(role -> ResponseEntity.ok(role))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/owner")
    public ResponseEntity<RoleDto> getOwnerRole() {
        return roleService.getOwnerRole()
            .map(role -> ResponseEntity.ok(role))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRoleRequest request) {
        RoleDto updatedRole = roleService.updateRole(id, request);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteRoles(@RequestBody List<UUID> ids) {
        roleService.deleteRoles(ids);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getRoleCount() {
        long count = roleService.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> checkRoleExists(@PathVariable UUID id) {
        boolean exists = roleService.existsById(id);
        return ResponseEntity.ok(exists);
    }
}
