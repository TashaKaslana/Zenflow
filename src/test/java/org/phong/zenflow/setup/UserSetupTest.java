package org.phong.zenflow.setup;

import org.phong.zenflow.user.dtos.CreateUserRequest;
import org.phong.zenflow.user.dtos.UserDto;
import org.phong.zenflow.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class UserSetupTest {
    @Autowired
    private UserService userService;

    @Autowired
    private TestDataConfiguration testDataConfig;

    public void setupUsers() {
        Map<String, UUID> userIdMap = new HashMap<>();

        testDataConfig.getUsers().getList().forEach(userConfig -> {
            CreateUserRequest req = new CreateUserRequest();
            req.setUsername(userConfig.getUsername());
            req.setEmail(userConfig.getEmail());
            req.setPasswordHash(userConfig.getPassword());
            req.setRoleName(userConfig.getRole());
            UserDto saved = userService.createUser(req);

            userIdMap.put(userConfig.getUsername(), saved.getId());
        });

        ContextSetupHolder.set("users", userIdMap);
    }
}
