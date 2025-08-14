package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IfNodeExecutorTest {

    private final IfNodeExecutor executor = new IfNodeExecutor();

    @Test
    void directsToTrueBranchWhenConditionMatches() {
        RuntimeContext context = new RuntimeContext();
        WorkflowConfig config = new WorkflowConfig(Map.of(
                "condition", "1 == 1",
                "next_true", List.of("true"),
                "next_false", List.of("false")
        ));

        var result = executor.execute(config, context);

        assertEquals(ExecutionStatus.NEXT, result.getStatus());
        assertEquals("true", result.getNextNodeKey());
    }

    @Test
    void directsToFalseBranchWhenConditionFails() {
        RuntimeContext context = new RuntimeContext();
        WorkflowConfig config = new WorkflowConfig(Map.of(
                "condition", "1 == 0",
                "next_true", List.of("true"),
                "next_false", List.of("false")
        ));

        var result = executor.execute(config, context);

        assertEquals(ExecutionStatus.NEXT, result.getStatus());
        assertEquals("false", result.getNextNodeKey());
    }
}

