package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder.PlaceholderExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderExecutorTest {
    private final PlaceholderExecutor executor = new PlaceholderExecutor();

    @Test
    void echoesInput() {
        WorkflowConfig config = new WorkflowConfig(Map.of("foo", "bar"));
        ExecutionContext ctx = TestExecutionContextUtils.createExecutionContext();

        ctx.setCurrentConfig(config);


        var result = executor.execute(ctx);
        TestExecutionContextUtils.flushPendingWrites(ctx);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("bar", ctx.read("foo", String.class));
    }
}
