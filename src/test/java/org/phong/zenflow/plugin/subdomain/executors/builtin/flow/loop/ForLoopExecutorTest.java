package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.loop;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_loop.ForLoopExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.context.ReadOptions;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

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
        
        // Initial config with loop parameters
        Map<String, Object> config = new HashMap<>();
        config.put("total", 3);
        config.put("updateExpression", "index + 1");
        config.put("next", List.of("body"));
        config.put("loopEnd", List.of("end"));
        
        // Set config once - it doesn't change during loop iterations
        context.setCurrentConfig(new WorkflowConfig(config));

        int iterations = 0;
        int maxIterations = 10; // Safety limit
        
        while (iterations < maxIterations) {
            var result = executor.execute(context);
            TestExecutionContextUtils.flushPendingWrites(context);

            if (result.getStatus() == ExecutionStatus.LOOP_NEXT) {
                iterations++;
            } else if (result.getStatus() == ExecutionStatus.LOOP_END) {
                assertEquals(3, iterations, "Expected 3 iterations before LOOP_END");
                assertEquals(3, context.read("index", Integer.class, ReadOptions.PREFER_CONTEXT).intValue(), "Expected final index to be 3");
                return; // Test passed
            } else {
                fail("Unexpected status: " + result.getStatus());
            }
        }
        
        fail("Loop exceeded maximum iterations (" + maxIterations + ")");
    }
}

