package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.wait;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.wait.WaitExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitExecutorTest {

    private final WaitExecutor executor = new WaitExecutor();

    @Test
    void commitWhenAllDependenciesReady() {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        Map<String, Object> input = new HashMap<>();
        input.put("mode", "all");
        input.put("waitingNodes", Map.of("A", true, "B", true));
        WorkflowConfig config = new WorkflowConfig(input);

        ExecutionResult result = executor.execute(config, context);

        assertEquals(ExecutionStatus.COMMIT, result.getStatus());
        assertTrue((Boolean) result.getOutput().get("isReady"));
    }

    @Test
    void timeoutWithoutFallbackReturnsError() throws InterruptedException {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        Map<String, Object> input = new HashMap<>();
        input.put("mode", "all");
        input.put("waitingNodes", Map.of("A", false));
        input.put("timeoutMs", 100L);
        WorkflowConfig config = new WorkflowConfig(input);

        ExecutionResult first = executor.execute(config, context);
        assertEquals(ExecutionStatus.UNCOMMIT, first.getStatus());

        Thread.sleep(150);

        ExecutionResult second = executor.execute(config, context);
        assertEquals(ExecutionStatus.ERROR, second.getStatus());
    }

    @Test
    void timeoutWithFallbackReturnsConfiguredStatus() throws InterruptedException {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        Map<String, Object> input = new HashMap<>();
        input.put("mode", "all");
        input.put("waitingNodes", Map.of("A", false));
        input.put("timeoutMs", 100L);
        input.put("fallbackStatus", "UNCOMMIT");
        WorkflowConfig config = new WorkflowConfig(input);

        ExecutionResult first = executor.execute(config, context);
        assertEquals(ExecutionStatus.UNCOMMIT, first.getStatus());

        Thread.sleep(150);

        ExecutionResult second = executor.execute(config, context);
        assertEquals(ExecutionStatus.UNCOMMIT, second.getStatus());
    }
}

