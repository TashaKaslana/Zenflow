package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.branch.executor;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.executor.SwitchNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.dto.SwitchCase;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwitchNodeExecutorTest {

    private final SwitchNodeExecutor executor = new SwitchNodeExecutor();

    @Test
    void routesToMatchingCase() {
        RuntimeContext context = new RuntimeContext();
        List<SwitchCase> cases = List.of(new SwitchCase("B", List.of("b")));
        WorkflowConfig config = new WorkflowConfig(Map.of(
                "expression", "B",
                "cases", cases,
                "default_case", "default"
        ));

        var result = executor.execute(config, context);

        assertEquals(ExecutionStatus.NEXT, result.getStatus());
        assertEquals("b", result.getNextNodeKey());
    }

    @Test
    void fallsBackToDefaultWhenNoCaseMatches() {
        RuntimeContext context = new RuntimeContext();
        List<SwitchCase> cases = List.of(new SwitchCase("A", List.of("a")));
        WorkflowConfig config = new WorkflowConfig(Map.of(
                "expression", "C",
                "cases", cases,
                "default_case", "default"
        ));

        var result = executor.execute(config, context);

        assertEquals(ExecutionStatus.NEXT, result.getStatus());
        assertEquals("default", result.getNextNodeKey());
    }
}

