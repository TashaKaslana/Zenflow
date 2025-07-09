package org.phong.zenflow.user.subdomain.role.dtos;

import lombok.Data;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class RoleDto {
    private UUID id;
    private UserRoleEnum name;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
