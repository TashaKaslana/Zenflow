package org.phong.zenflow.user.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.user.dtos.CreateUserRequest;
import org.phong.zenflow.user.dtos.UserDto;
import org.phong.zenflow.user.service.UserPermissionService;
import org.phong.zenflow.user.service.UserService;
import org.phong.zenflow.user.subdomain.permission.dtos.PermissionDto;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPermissionService userPermissionService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<UserDto>> createUsers(@Valid @RequestBody List<CreateUserRequest> requests) {
        List<UserDto> createdUsers = userService.createUsers(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUsers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        return userService.findById(id)
            .map(user -> ResponseEntity.ok(user))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        Page<UserDto> users = userService.findAll(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email)
            .map(user -> ResponseEntity.ok(user))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
            .map(user -> ResponseEntity.ok(user))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{roleName}")
    public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable UserRoleEnum roleName) {
        List<UserDto> users = userService.findByRole(roleName);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/emails")
    public ResponseEntity<List<UserDto>> getUsersByEmails(@RequestBody List<String> emails) {
        List<UserDto> users = userService.findByEmails(emails);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/usernames")
    public ResponseEntity<List<UserDto>> getUsersByUsernames(@RequestBody List<String> usernames) {
        List<UserDto> users = userService.findByUsernames(usernames);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserDto>> getActiveUsers() {
        List<UserDto> users = userService.findActiveUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<UserDto>> getDeletedUsers() {
        List<UserDto> users = userService.findDeletedUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody CreateUserRequest request) {
        UserDto updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<UserDto> updateUserPassword(@PathVariable UUID id, @RequestBody String newPasswordHash) {
        UserDto updatedUser = userService.updatePassword(id, newPasswordHash);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> changeUserRole(@PathVariable UUID id, @RequestBody UserRoleEnum newRole) {
        UserDto updatedUser = userService.changeUserRole(id, newRole);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteUsers(@RequestBody List<UUID> ids) {
        userService.deleteUsers(ids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable UUID id) {
        userService.hardDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<UserDto> restoreUser(@PathVariable UUID id) {
        UserDto restoredUser = userService.restoreUser(id);
        return ResponseEntity.ok(restoredUser);
    }

    @GetMapping("/check/email/{email}")
    public ResponseEntity<Boolean> checkEmailTaken(@PathVariable String email) {
        boolean taken = userService.isEmailTaken(email);
        return ResponseEntity.ok(taken);
    }

    @GetMapping("/check/username/{username}")
    public ResponseEntity<Boolean> checkUsernameTaken(@PathVariable String username) {
        boolean taken = userService.isUsernameTaken(username);
        return ResponseEntity.ok(taken);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getUserCount() {
        long count = userService.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/emails")
    public ResponseEntity<List<String>> getAllEmails() {
        List<String> emails = userService.getAllEmails();
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/usernames")
    public ResponseEntity<List<String>> getAllUsernames() {
        List<String> usernames = userService.getAllUsernames();
        return ResponseEntity.ok(usernames);
    }

    // User Permission endpoints
    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<PermissionDto>> getUserPermissions(@PathVariable UUID id) {
        List<PermissionDto> permissions = userPermissionService.getUserPermissions(id);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{id}/permissions/check")
    public ResponseEntity<Boolean> checkUserPermission(
            @PathVariable UUID id,
            @RequestParam String feature,
            @RequestParam String action) {
        boolean hasPermission = userPermissionService.userHasPermission(id, feature, action);
        return ResponseEntity.ok(hasPermission);
    }

    @PostMapping("/{id}/permissions/check-any")
    public ResponseEntity<Boolean> checkUserHasAnyPermission(
            @PathVariable UUID id,
            @RequestParam List<String> features,
            @RequestParam List<String> actions) {
        boolean hasAnyPermission = userPermissionService.userHasAnyPermission(id, features, actions);
        return ResponseEntity.ok(hasAnyPermission);
    }

    @PostMapping("/{id}/permissions/check-all")
    public ResponseEntity<Boolean> checkUserHasAllPermissions(
            @PathVariable UUID id,
            @RequestParam List<String> features,
            @RequestParam List<String> actions) {
        boolean hasAllPermissions = userPermissionService.userHasAllPermissions(id, features, actions);
        return ResponseEntity.ok(hasAllPermissions);
    }
}
