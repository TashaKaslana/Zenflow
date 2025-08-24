package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_loop.ForLoopExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ForLoopExecutorTest {

    private final ForLoopExecutor executor = new ForLoopExecutor();

    @Test
    void iteratesThreeTimesThenEnds() {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        Map<String, Object> input = new HashMap<>();
        input.put("index", 0);
        input.put("total", 3);
        input.put("updateExpression", "index + 1");
        input.put("next", List.of("body"));
        input.put("loopEnd", List.of("end"));

        int iterations = 0;
        while (true) {
            WorkflowConfig config = new WorkflowConfig(new HashMap<>(input));
            var result = executor.execute(config, context);

            if (result.getStatus() == ExecutionStatus.LOOP_NEXT) {
                iterations++;
                input.put("index", result.getOutput().get("index"));
            } else if (result.getStatus() == ExecutionStatus.LOOP_END) {
                assertEquals(3, iterations);
                assertEquals(3, ((Number) result.getOutput().get("index")).intValue());
                break;
            } else {
                fail("Unexpected status: " + result.getStatus());
            }
        }
    }
}

