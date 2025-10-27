package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.number_validator.NumberValidatorExecutor;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.context.ReadOptions;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NumberValidatorExecutorTest {

    private final NumberValidatorExecutor executor = new NumberValidatorExecutor();

    @Test
    void validatesWithinThreshold() {
        WorkflowConfig config = new WorkflowConfig(
                Map.of(
                        "number", 5,
                        "threshold", 10
                )
        );
        var context = TestExecutionContextUtils.createExecutionContext(config);
        var result = executor.execute(context);
        TestExecutionContextUtils.flushPendingWrites(context);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertTrue(context.read("valid", Boolean.class, ReadOptions.PREFER_CONTEXT));
    }

    @Test
    void detectsExceedingThreshold() {
        WorkflowConfig config = new WorkflowConfig(
                Map.of(
                        "number", 15,
                        "threshold", 10
                )
        );
        var context = TestExecutionContextUtils.createExecutionContext(config);
        var result = executor.execute(context);
        TestExecutionContextUtils.flushPendingWrites(context);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertFalse(context.read("valid", Boolean.class, ReadOptions.PREFER_CONTEXT));
    }
}
