package org.phong.zenflow.user.infrastructure.persistence.repositories;

import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.user.infrastructure.persistence.projections.UserEmailProjection;
import org.phong.zenflow.user.infrastructure.persistence.projections.UserUsernameProjection;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Find by email
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(@Param("email") String email);

    // Find by username
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsername(@Param("username") String username);

    // Find by role
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    List<User> findByRole(@Param("role") Role role);

    // Find active users
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findActiveUsers();

    // Find deleted users
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL")
    List<User> findDeletedUsers();

    // Check existence by email
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    // Check existence by username
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    boolean existsByUsername(@Param("username") String username);

    // Bulk operations
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.deletedAt IS NULL")
    List<User> findByRoleIn(@Param("roles") List<Role> roles);

    @Query("SELECT u FROM User u WHERE u.email IN :emails AND u.deletedAt IS NULL")
    List<User> findByEmailIn(@Param("emails") List<String> emails);

    @Query("SELECT u FROM User u WHERE u.username IN :usernames AND u.deletedAt IS NULL")
    List<User> findByUsernameIn(@Param("usernames") List<String> usernames);

    // Projections for efficient queries
    @Query("SELECT DISTINCT u.email FROM User u WHERE u.deletedAt IS NULL")
    List<UserEmailProjection> findAllEmails();

    @Query("SELECT DISTINCT u.username FROM User u WHERE u.deletedAt IS NULL")
    List<UserUsernameProjection> findAllUsernames();
}