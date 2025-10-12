package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder.PlaceholderExecutor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        PlaceholderExecutor.class,
        PluginNodeExecutorRegistry.class
})
class PlaceholderExecutorRegistrationTest {

    @Autowired
    private PluginNodeExecutorRegistry registry;

    @Autowired
    private ApplicationContext applicationContext;

    private final String placeholderUuid = "123e4567-e89b-12d3-a456-426614174001";
    private final String placeholderCompositeKey = "test:placeholder:1.0.0";

    @BeforeEach
    void setUp() {
        // Directly register the PlaceholderExecutor with both UUID and composite key
        // This simulates what the PluginNodeSynchronizer would do when it processes the @PluginNode annotation
        PlaceholderExecutor executor = applicationContext.getBean(PlaceholderExecutor.class);
        NodeDefinition definition = NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
        registry.register(placeholderUuid, () -> definition);
        registry.register(placeholderCompositeKey, () -> definition);
    }

    @Test
    void placeholderExecutorIsRegistered() {
        // Test UUID-based registration
        assertTrue(registry.getDefinition(placeholderUuid).isPresent(),
                "Placeholder executor should be registered with UUID");

        // Test composite key fallback
        assertTrue(registry.getDefinition(placeholderCompositeKey).isPresent(),
                "Placeholder executor should be registered with composite key");
    }

    @Test
    void placeholderExecutorExecutesCorrectly() {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        Map<String, Object> inputData = Map.of("testKey", "testValue");
        WorkflowConfig config = new WorkflowConfig(inputData, Map.of());

        var definition = registry.getDefinition(placeholderUuid).orElseThrow();
        ExecutionResult result;

        try {
            context.setCurrentConfig(config);
            result = definition.getNodeExecutor().execute(context);
        } catch (Exception e) {
            result = null;
        }

        assertNotNull(result);
        assertEquals(inputData, result.getOutput());
    }
}
