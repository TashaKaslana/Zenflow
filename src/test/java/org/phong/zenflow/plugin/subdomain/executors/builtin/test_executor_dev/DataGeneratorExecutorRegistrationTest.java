package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.register.ExecutorInitializer;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        DataGeneratorExecutor.class,
        ExecutorInitializer.class,
        PluginNodeExecutorRegistry.class
})
class DataGeneratorExecutorRegistrationTest {

    @Autowired
    private PluginNodeExecutorRegistry registry;

    @Test
    void dataGeneratorExecutorRegistered() {
        assertThat(registry.getExecutor("core:data.generate:1.0.0")).isPresent();
    }
}
