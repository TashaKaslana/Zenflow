package org.phong.zenflow.user.subdomain.role.dtos;

import lombok.Data;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;

@Data
public class CreateRoleRequest {
    private UserRoleEnum name;
    private String description;
}
