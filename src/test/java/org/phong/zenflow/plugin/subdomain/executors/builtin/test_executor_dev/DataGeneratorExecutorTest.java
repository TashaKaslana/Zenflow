package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.data_generator.DataGeneratorExecutor;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

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
        var context = TestExecutionContextUtils.createExecutionContext(config);
        var result = executor.execute(context);
        TestExecutionContextUtils.flushPendingWrites(context);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("test+tag@very-long-domain-name.example.org", context.read("user_email", String.class));
        assertEquals(123, context.read("user_age", Integer.class));
        assertEquals(true, context.read("user_active", Boolean.class));
    }
}
