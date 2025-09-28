package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.text_processor.TextProcessorExecutor;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextProcessorExecutorTest {

    private final TextProcessorExecutor executor = new TextProcessorExecutor();

    @Test
    void processesTextSuccessfully() {
        WorkflowConfig config = new WorkflowConfig(
                Map.of(
                        "text", "hello",
                        "count", 3,
                        "flag", true
                )
        );
        var result = executor.execute(config, TestExecutionContextUtils.createExecutionContext());

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("Processed text: hello", result.getOutput().get("result"));
        assertEquals(3, result.getOutput().get("processed_count"));
    }
}
