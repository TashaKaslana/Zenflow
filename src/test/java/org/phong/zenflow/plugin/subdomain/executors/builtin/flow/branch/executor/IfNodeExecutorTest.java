package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.if_node.IfNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IfNodeExecutorTest {

    private final IfNodeExecutor executor = new IfNodeExecutor();

    @Test
    void directsToTrueBranchWhenConditionMatches() {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        WorkflowConfig config = new WorkflowConfig(Map.of(
                "condition", "1 == 1",
                "next_true", List.of("true"),
                "next_false", List.of("false")
        ));

        context.setCurrentConfig(config);


        var result = executor.execute(context);

        assertEquals(ExecutionStatus.NEXT, result.getStatus());
        assertEquals("true", result.getNextNodeKey());
    }

    @Test
    void directsToFalseBranchWhenConditionFails() {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        WorkflowConfig config = new WorkflowConfig(Map.of(
                "condition", "1 == 0",
                "next_true", List.of("true"),
                "next_false", List.of("false")
        ));

        context.setCurrentConfig(config);


        var result = executor.execute(context);

        assertEquals(ExecutionStatus.NEXT, result.getStatus());
        assertEquals("false", result.getNextNodeKey());
    }
}

