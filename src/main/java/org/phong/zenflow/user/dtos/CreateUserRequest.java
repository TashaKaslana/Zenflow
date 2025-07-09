package org.phong.zenflow.user.dtos;

import lombok.Data;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String passwordHash;
    private UserRoleEnum roleName;
}
