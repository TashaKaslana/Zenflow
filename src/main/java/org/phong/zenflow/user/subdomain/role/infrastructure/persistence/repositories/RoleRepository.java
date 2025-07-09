package org.phong.zenflow.user.subdomain.role.infrastructure.persistence.repositories;

import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.phong.zenflow.user.subdomain.role.infrastructure.persistence.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Find by name
    Optional<Role> findByName(UserRoleEnum name);

    // Check existence by name
    boolean existsByName(UserRoleEnum name);

    // Bulk operations
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNameIn(@Param("names") List<UserRoleEnum> names);

    @Query("SELECT r FROM Role r WHERE r.id IN :ids")
    List<Role> findByIdIn(@Param("ids") List<UUID> ids);
}