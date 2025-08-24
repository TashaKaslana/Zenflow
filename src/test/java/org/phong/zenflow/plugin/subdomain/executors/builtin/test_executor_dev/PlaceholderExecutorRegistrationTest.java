package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder.PlaceholderExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.phong.zenflow.plugin.subdomain.execution.register.ExecutorInitializer;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(registry.getExecutor(PluginNodeIdentifier.fromString("test:placeholder:1.0.0")).isPresent());
    }

    @Test
    void isSendBackData() {
        var executor = registry.getExecutor(PluginNodeIdentifier.fromString("test:placeholder:1.0.0")).orElseThrow();
        WorkflowConfig resolvedConfig = new WorkflowConfig(
                Map.of(
                        "input1", "value1",
                        "input2", 42,
                        "input3", true
                )
        );
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        var result = executor.execute(resolvedConfig, context);

        assertEquals("value1", result.getOutput().get("input1"));
        assertEquals(42, result.getOutput().get("input2"));
        assertEquals(true, result.getOutput().get("input3"));
    }
}
