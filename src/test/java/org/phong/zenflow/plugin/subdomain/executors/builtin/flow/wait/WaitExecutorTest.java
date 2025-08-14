package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.wait;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionInput;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.dto.RuntimeMetadata;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextPool;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitExecutorTest {

    private final WaitExecutor executor = new WaitExecutor();

    @Test
    void commitWhenAllDependenciesReady() {
        RuntimeContext context = new RuntimeContext();
        UUID runId = UUID.randomUUID();
        RuntimeContextPool.registerContext(runId, context);
        Map<String, Object> input = new HashMap<>();
        input.put("mode", "all");
        input.put("waitingNodes", Map.of("A", true, "B", true));
        WorkflowConfig config = new WorkflowConfig(input);
        ExecutionInput executionInput = new ExecutionInput(config, new RuntimeMetadata(runId));

        ExecutionResult result = executor.execute(executionInput);

        assertEquals(ExecutionStatus.COMMIT, result.getStatus());
        assertTrue((Boolean) result.getOutput().get("isReady"));
    }

    @Test
    void timeoutWithoutFallbackReturnsError() throws InterruptedException {
        RuntimeContext context = new RuntimeContext();
        UUID runId = UUID.randomUUID();
        RuntimeContextPool.registerContext(runId, context);
        Map<String, Object> input = new HashMap<>();
        input.put("mode", "all");
        input.put("waitingNodes", Map.of("A", false));
        input.put("timeoutMs", 100L);
        WorkflowConfig config = new WorkflowConfig(input);
        ExecutionInput executionInput = new ExecutionInput(config, new RuntimeMetadata(runId));

        ExecutionResult first = executor.execute(executionInput);
        assertEquals(ExecutionStatus.UNCOMMIT, first.getStatus());

        Thread.sleep(150);

        ExecutionResult second = executor.execute(executionInput);
        assertEquals(ExecutionStatus.ERROR, second.getStatus());
    }

    @Test
    void timeoutWithFallbackReturnsConfiguredStatus() throws InterruptedException {
        RuntimeContext context = new RuntimeContext();
        UUID runId = UUID.randomUUID();
        RuntimeContextPool.registerContext(runId, context);
        Map<String, Object> input = new HashMap<>();
        input.put("mode", "all");
        input.put("waitingNodes", Map.of("A", false));
        input.put("timeoutMs", 100L);
        input.put("fallbackStatus", "UNCOMMIT");
        WorkflowConfig config = new WorkflowConfig(input);
        ExecutionInput executionInput = new ExecutionInput(config, new RuntimeMetadata(runId));

        ExecutionResult first = executor.execute(executionInput);
        assertEquals(ExecutionStatus.UNCOMMIT, first.getStatus());

        Thread.sleep(150);

        ExecutionResult second = executor.execute(executionInput);
        assertEquals(ExecutionStatus.UNCOMMIT, second.getStatus());
    }
}

