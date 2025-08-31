package org.phong.zenflow.setup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.user.subdomain.role.enums.UserRoleEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Externalized test data configuration to make setup more maintainable
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "test.setup")
public class TestDataConfiguration {

    // Main getters and setters
    private Users users = new Users();
    private Projects projects = new Projects();
    private Workflows workflows = new Workflows();

    @Data
    public static class Users {
        private List<UserConfig> list = List.of(
            new UserConfig("admin", "admin@gmail.com", "admin123", UserRoleEnum.ADMIN),
            new UserConfig("user", "user@gmail.com", "user123", UserRoleEnum.USER)
        );
    }

    @Data
    @AllArgsConstructor
    public static class UserConfig {
        // Getters and setters
        private String username;
        private String email;
        private String password;
        private UserRoleEnum role;
    }

    @Data
    public static class Projects {
        private Map<String, String> descriptions = Map.of(
            "alpha", "First project description",
            "beta", "Second project description"
        );
    }

    @Data
    public static class Workflows {
        private Map<String, String> descriptions = Map.of(
            "standard-workflow", "Initial workflow for alpha project",
            "beta-workflow", "Initial workflow for beta project"
        );
    }
}
