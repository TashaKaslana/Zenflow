package org.phong.zenflow.user.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.user.dtos.CreateUserRequest;
import org.phong.zenflow.user.dtos.UserDto;
import org.phong.zenflow.user.service.UserPermissionService;
import org.phong.zenflow.user.service.UserService;
import org.phong.zenflow.user.subdomain.permission.dtos.PermissionDto;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPermissionService userPermissionService;

    @PostMapping
    public ResponseEntity<RestApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto createdUser = userService.createUser(request);
        return RestApiResponse.created(createdUser, "User created successfully");
    }

    @PostMapping("/bulk")
    public ResponseEntity<RestApiResponse<List<UserDto>>> createUsers(@Valid @RequestBody List<CreateUserRequest> requests) {
        List<UserDto> createdUsers = userService.createUsers(requests);
        return RestApiResponse.created(createdUsers, "Users created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.findById(id);
        return RestApiResponse.success(user, "User retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.findAll();
        return RestApiResponse.success(users, "Users retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<UserDto>>> getAllUsers(Pageable pageable) {
        Page<UserDto> users = userService.findAll(pageable);
        return RestApiResponse.success(users, "Users retrieved successfully");
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<RestApiResponse<UserDto>> getUserByEmail(@PathVariable String email) {
        UserDto user = userService.findByEmail(email);
        return RestApiResponse.success(user, "User retrieved successfully");
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<RestApiResponse<UserDto>> getUserByUsername(@PathVariable String username) {
        UserDto user = userService.findByUsername(username);
        return RestApiResponse.success(user, "User retrieved successfully");
    }

    @GetMapping("/role/{roleName}")
    public ResponseEntity<RestApiResponse<List<UserDto>>> getUsersByRole(@PathVariable UserRoleEnum roleName) {
        List<UserDto> users = userService.findByRole(roleName);
        return RestApiResponse.success(users, "Users retrieved successfully");
    }

    @PostMapping("/emails")
    public ResponseEntity<RestApiResponse<List<UserDto>>> getUsersByEmails(@RequestBody List<String> emails) {
        List<UserDto> users = userService.findByEmails(emails);
        return RestApiResponse.success(users, "Users retrieved successfully");
    }

    @PostMapping("/usernames")
    public ResponseEntity<RestApiResponse<List<UserDto>>> getUsersByUsernames(@RequestBody List<String> usernames) {
        List<UserDto> users = userService.findByUsernames(usernames);
        return RestApiResponse.success(users, "Users retrieved successfully");
    }

    @GetMapping("/active")
    public ResponseEntity<RestApiResponse<List<UserDto>>> getActiveUsers() {
        List<UserDto> users = userService.findActiveUsers();
        return RestApiResponse.success(users, "Active users retrieved successfully");
    }

    @GetMapping("/deleted")
    public ResponseEntity<RestApiResponse<List<UserDto>>> getDeletedUsers() {
        List<UserDto> users = userService.findDeletedUsers();
        return RestApiResponse.success(users, "Deleted users retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<UserDto>> updateUser(@PathVariable UUID id, @Valid @RequestBody CreateUserRequest request) {
        UserDto updatedUser = userService.updateUser(id, request);
        return RestApiResponse.success(updatedUser, "User updated successfully");
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<RestApiResponse<UserDto>> updateUserPassword(@PathVariable UUID id, @RequestBody String newPasswordHash) {
        UserDto updatedUser = userService.updatePassword(id, newPasswordHash);
        return RestApiResponse.success(updatedUser, "Password updated successfully");
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<RestApiResponse<UserDto>> changeUserRole(@PathVariable UUID id, @RequestBody UserRoleEnum newRole) {
        UserDto updatedUser = userService.changeUserRole(id, newRole);
        return RestApiResponse.success(updatedUser, "User role changed successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return RestApiResponse.success("User deleted successfully");
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<RestApiResponse<Void>> deleteUsers(@RequestBody List<UUID> ids) {
        userService.deleteUsers(ids);
        return RestApiResponse.success("Users deleted successfully");
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<RestApiResponse<Void>> hardDeleteUser(@PathVariable UUID id) {
        userService.hardDeleteUser(id);
        return RestApiResponse.success("User permanently deleted successfully");
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<RestApiResponse<UserDto>> restoreUser(@PathVariable UUID id) {
        UserDto restoredUser = userService.restoreUser(id);
        return RestApiResponse.success(restoredUser, "User restored successfully");
    }

    @GetMapping("/check/email/{email}")
    public ResponseEntity<RestApiResponse<Boolean>> checkEmailTaken(@PathVariable String email) {
        boolean taken = userService.isEmailTaken(email);
        return RestApiResponse.success(taken, "Email availability checked");
    }

    @GetMapping("/check/username/{username}")
    public ResponseEntity<RestApiResponse<Boolean>> checkUsernameTaken(@PathVariable String username) {
        boolean taken = userService.isUsernameTaken(username);
        return RestApiResponse.success(taken, "Username availability checked");
    }

    @GetMapping("/count")
    public ResponseEntity<RestApiResponse<Long>> getUserCount() {
        long count = userService.count();
        return RestApiResponse.success(count, "User count retrieved successfully");
    }

    @GetMapping("/emails")
    public ResponseEntity<RestApiResponse<List<String>>> getAllEmails() {
        List<String> emails = userService.getAllEmails();
        return RestApiResponse.success(emails, "All emails retrieved successfully");
    }

    @GetMapping("/usernames")
    public ResponseEntity<RestApiResponse<List<String>>> getAllUsernames() {
        List<String> usernames = userService.getAllUsernames();
        return RestApiResponse.success(usernames, "All usernames retrieved successfully");
    }

    // User Permission endpoints
    @GetMapping("/{id}/permissions")
    public ResponseEntity<RestApiResponse<List<PermissionDto>>> getUserPermissions(@PathVariable UUID id) {
        List<PermissionDto> permissions = userPermissionService.getUserPermissions(id);
        return RestApiResponse.success(permissions, "User permissions retrieved successfully");
    }

    @GetMapping("/{id}/permissions/check")
    public ResponseEntity<RestApiResponse<Boolean>> checkUserPermission(
            @PathVariable UUID id,
            @RequestParam String feature,
            @RequestParam String action) {
        boolean hasPermission = userPermissionService.userHasPermission(id, feature, action);
        return RestApiResponse.success(hasPermission, "User permission checked");
    }

    @PostMapping("/{id}/permissions/check-any")
    public ResponseEntity<RestApiResponse<Boolean>> checkUserHasAnyPermission(
            @PathVariable UUID id,
            @RequestParam List<String> features,
            @RequestParam List<String> actions) {
        boolean hasAnyPermission = userPermissionService.userHasAnyPermission(id, features, actions);
        return RestApiResponse.success(hasAnyPermission, "User permissions checked");
    }

    @PostMapping("/{id}/permissions/check-all")
    public ResponseEntity<RestApiResponse<Boolean>> checkUserHasAllPermissions(
            @PathVariable UUID id,
            @RequestParam List<String> features,
            @RequestParam List<String> actions) {
        boolean hasAllPermissions = userPermissionService.userHasAllPermissions(id, features, actions);
        return RestApiResponse.success(hasAllPermissions, "User permissions checked");
    }
}
