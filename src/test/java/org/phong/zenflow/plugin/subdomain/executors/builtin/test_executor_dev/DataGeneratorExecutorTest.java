package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataGeneratorExecutorTest {

    private final DataGeneratorExecutor executor = new DataGeneratorExecutor();

    @Test
    void generatesMockData() {
        WorkflowConfig config = new WorkflowConfig(
                Map.of(
                        "seed", 1,
                        "format", "json"
                )
        );
        var result = executor.execute(config, new RuntimeContext());

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("test+tag@very-long-domain-name.example.org", result.getOutput().get("user_email"));
        assertEquals(123, result.getOutput().get("user_age"));
        assertEquals(true, result.getOutput().get("user_active"));
    }
}
