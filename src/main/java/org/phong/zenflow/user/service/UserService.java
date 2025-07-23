package org.phong.zenflow.user.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.user.dtos.CreateUserRequest;
import org.phong.zenflow.user.dtos.UpdateUserRequest;
import org.phong.zenflow.user.dtos.UserDto;
import org.phong.zenflow.user.exception.UserEmailExistsException;
import org.phong.zenflow.user.exception.UserNotFoundException;
import org.phong.zenflow.user.exception.UserUsernameExistsException;
import org.phong.zenflow.user.infrastructure.mapstruct.UserMapper;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.user.infrastructure.persistence.projections.UserEmailProjection;
import org.phong.zenflow.user.infrastructure.persistence.projections.UserUsernameProjection;
import org.phong.zenflow.user.infrastructure.persistence.repositories.UserRepository;
import org.phong.zenflow.user.subdomain.role.exception.RoleNotFoundException;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.Role;
import org.phong.zenflow.user.subdomain.role.service.RoleService;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;

    /**
     * Create a new user using DTO
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_CREATE, targetIdExpression = "returnObject.id")
    public UserDto createUser(CreateUserRequest request) {
        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserEmailExistsException(request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserUsernameExistsException(request.getUsername());
        }

        // Get role entity
        Role role = roleService.findEntityByName(request.getRoleName() != null ? request.getRoleName() : UserRoleEnum.USER)
            .orElseThrow(() -> new RoleNotFoundException("name", request.getRoleName()));

        User user = userMapper.toEntity(request);
        user.setRole(role);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    /**
     * Create multiple users in bulk
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_CREATE, description = "Bulk user creation", targetIdExpression = "returnObject.![id]")
    public List<UserDto> createUsers(List<CreateUserRequest> requests) {
        // Validate all requests first
        for (CreateUserRequest request : requests) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserEmailExistsException(request.getEmail());
            }
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserUsernameExistsException(request.getUsername());
            }
        }

        List<User> users = requests.stream()
            .map(request -> {
                Role role = roleService.findEntityByName(request.getRoleName() != null ? request.getRoleName() : UserRoleEnum.USER)
                    .orElseThrow(() -> new RoleNotFoundException("name", request.getRoleName()));

                User user = userMapper.toEntity(request);
                user.setRole(role);
                return user;
            })
            .toList();

        List<User> savedUsers = userRepository.saveAll(users);
        return userMapper.toDtoList(savedUsers);
    }

    /**
     * Find user by ID
     */
    public UserDto findById(UUID id) {
        return userRepository.findById(id)
            .map(userMapper::toDto)
            .orElseThrow(() -> new UserNotFoundException(id.toString()));
    }

    /**
     * Find all users
     */
    public List<UserDto> findAll() {
        List<User> users = userRepository.findAll();
        return userMapper.toDtoList(users);
    }

    /**
     * Find users with pagination
     */
    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(userMapper::toDto);
    }

    /**
     * Update user using DTO
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_UPDATE, targetIdExpression = "#id")
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id.toString()));

        // Check if another user takes email/username
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new UserEmailExistsException(request.email());
        }
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new UserUsernameExistsException(request.username());
        }

        // Get role if specified
        if (request.roleName() != null) {
            Role role = roleService.findEntityByName(request.roleName())
                .orElseThrow(() -> new RoleNotFoundException("name", request.roleName()));
            user.setRole(role);
        }

        userMapper.updateEntity(request, user);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    /**
     * Update user password
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_UPDATE, description = "Password update", targetIdExpression = "#id")
    public UserDto updatePassword(UUID id, String newPasswordHash) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id.toString()));

        user.setPasswordHash(newPasswordHash);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    /**
     * Soft delete user
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_DELETE, targetIdExpression = "#id")
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id.toString()));

        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    /**
     * Delete multiple users in bulk
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_DELETE, description = "Bulk user deletion", targetIdExpression = "#ids")
    public void deleteUsers(List<UUID> ids) {
        List<User> users = userRepository.findByIdIn(ids);
        OffsetDateTime now = OffsetDateTime.now();
        users.forEach(user -> user.setDeletedAt(now));
        userRepository.saveAll(users);
    }

    /**
     * Hard delete user
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_DELETE, description = "Hard user deletion", targetIdExpression = "#id")
    public void hardDeleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    /**
     * Check if user exists
     */
    public boolean existsById(UUID id) {
        return userRepository.existsById(id);
    }

    /**
     * Count all users
     */
    public long count() {
        return userRepository.count();
    }

    /**
     * Find user by email using repository method
     */
    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(userMapper::toDto)
            .orElseThrow(() -> new UserNotFoundException("email", email));
    }

    /**
     * Find user by username using repository method
     */
    public UserDto findByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(userMapper::toDto)
            .orElseThrow(() -> new UserNotFoundException("username", username));
    }

    /**
     * Find users by role using repository method
     */
    public List<UserDto> findByRole(UserRoleEnum roleName) {
        Role role = roleService.findEntityByName(roleName)
            .orElseThrow(() -> new RoleNotFoundException("name", roleName));

        List<User> users = userRepository.findByRole(role);
        return userMapper.toDtoList(users);
    }

    /**
     * Find users by multiple emails in bulk
     */
    public List<UserDto> findByEmails(List<String> emails) {
        List<User> users = userRepository.findByEmailIn(emails);
        return userMapper.toDtoList(users);
    }

    /**
     * Find users by multiple usernames in bulk
     */
    public List<UserDto> findByUsernames(List<String> usernames) {
        List<User> users = userRepository.findByUsernameIn(usernames);
        return userMapper.toDtoList(users);
    }

    /**
     * Find active users using repository method
     */
    public List<UserDto> findActiveUsers() {
        List<User> users = userRepository.findActiveUsers();
        return userMapper.toDtoList(users);
    }

    /**
     * Find deleted users using repository method
     */
    public List<UserDto> findDeletedUsers() {
        List<User> users = userRepository.findDeletedUsers();
        return userMapper.toDtoList(users);
    }

    /**
     * Restore deleted user
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_UPDATE, description = "User restoration", targetIdExpression = "#id")
    public UserDto restoreUser(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id.toString()));

        user.setDeletedAt(null);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    /**
     * Check if email is already taken using repository method
     */
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if username is already taken using repository method
     */
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Change user role
     */
    @Transactional
    @AuditLog(action = AuditAction.USER_UPDATE, description = "User role change", targetIdExpression = "#userId")
    public UserDto changeUserRole(UUID userId, UserRoleEnum newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        Role role = roleService.findEntityByName(newRole)
            .orElseThrow(() -> new RoleNotFoundException("name", newRole));

        user.setRole(role);
        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    /**
     * Get all unique emails using projection for efficiency
     */
    public List<String> getAllEmails() {
        return userRepository.findAllEmails().stream()
            .map(UserEmailProjection::getEmail)
            .toList();
    }

    /**
     * Get all unique usernames using projection for efficiency
     */
    public List<String> getAllUsernames() {
        return userRepository.findAllUsernames().stream()
            .map(UserUsernameProjection::getUsername)
            .toList();
    }

    public User getReferenceById(UUID id) {
        return userRepository.getReferenceById(id);
    }
}
