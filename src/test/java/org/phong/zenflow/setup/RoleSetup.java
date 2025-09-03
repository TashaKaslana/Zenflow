package org.phong.zenflow.setup;

import org.phong.zenflow.user.subdomain.role.dtos.CreateRoleRequest;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.phong.zenflow.user.subdomain.role.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class RoleSetup {
    @Autowired
    private RoleService roleService;

    public void setupRoles() {
        Set<CreateRoleRequest> roles = new HashSet<>();

        CreateRoleRequest adminRole = new CreateRoleRequest();
        adminRole.setName(UserRoleEnum.ADMIN);
        adminRole.setDescription("Administrator with full access");
        roles.add(adminRole);

        CreateRoleRequest userRole = new CreateRoleRequest();
        userRole.setName(UserRoleEnum.USER);
        userRole.setDescription("Regular user with limited access");
        roles.add(userRole);

        roles.forEach(roleService::createRole);

        System.out.println("✅ Roles setup completed successfully!");
        System.out.println("Roles: ADMIN, USER, GUEST");
    }
}
