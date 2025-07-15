package org.phong.zenflow.user.dtos;

import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * DTO for {@link org.phong.zenflow.user.infrastructure.persistence.entities.User}
 */
public record UpdateUserRequest(String username, String email, String passwordHash,
                                UserRoleEnum roleName, OffsetDateTime deletedAt) implements Serializable {
}