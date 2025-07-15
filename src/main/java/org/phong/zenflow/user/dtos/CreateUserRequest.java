package org.phong.zenflow.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;

@Data
public class CreateUserRequest {
    @NotNull
    private String username;

    @Email
    private String email;

    @NotNull @NotBlank @Min(6) @Max(24)
    private String passwordHash;

    @NotNull
    private UserRoleEnum roleName = UserRoleEnum.USER;
}
