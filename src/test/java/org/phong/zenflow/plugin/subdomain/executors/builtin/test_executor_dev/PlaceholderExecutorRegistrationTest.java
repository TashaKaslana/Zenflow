package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.phong.zenflow.plugin.subdomain.execution.register.ExecutorInitializer;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {
        PlaceholderExecutor.class,
        PluginNodeExecutorRegistry.class,
        ExecutorInitializer.class
})
class PlaceholderExecutorRegistrationTest {

    @Autowired
    private PluginNodeExecutorRegistry registry;

    @Test
    void placeholderExecutorIsRegistered() {
        assertTrue(registry.getExecutor("core:placeholder:1.0.0").isPresent());
    }
}
