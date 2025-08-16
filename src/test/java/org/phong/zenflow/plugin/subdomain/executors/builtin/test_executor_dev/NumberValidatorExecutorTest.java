package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.number_validator.NumberValidatorExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

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
        var result = executor.execute(config, new RuntimeContext());

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertTrue((Boolean) result.getOutput().get("valid"));
    }

    @Test
    void detectsExceedingThreshold() {
        WorkflowConfig config = new WorkflowConfig(
                Map.of(
                        "number", 15,
                        "threshold", 10
                )
        );
        var result = executor.execute(config, new RuntimeContext());

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertFalse((Boolean) result.getOutput().get("valid"));
    }
}
