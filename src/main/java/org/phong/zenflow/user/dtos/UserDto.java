package org.phong.zenflow.user.dtos;

import lombok.Data;
import org.phong.zenflow.user.subdomain.role.dtos.RoleDto;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private RoleDto role;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
