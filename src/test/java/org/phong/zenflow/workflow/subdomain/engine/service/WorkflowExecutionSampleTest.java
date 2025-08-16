package org.phong.zenflow.workflow.subdomain.engine.service;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.register.ExecutorInitializer;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder.PlaceholderExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.ForLoopExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.if_node.IfNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node.SwitchNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node.SwitchCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {
        ForLoopExecutor.class,
        IfNodeExecutor.class,
        SwitchNodeExecutor.class,
        PlaceholderExecutor.class,
        PluginNodeExecutorRegistry.class,
        ExecutorInitializer.class
})
class WorkflowExecutionSampleTest {

    @Autowired
    private PluginNodeExecutorRegistry registry;

    @Test
    void executesWorkflowWithLoopAndBranches() {
        RuntimeContext context = new RuntimeContext();
        List<String> nodeKeys = List.of(
                "core:flow.loop.for:1.0.0",
                "core:placeholder:1.0.0",
                "core:flow.loop.for:1.0.0",
                "core:placeholder:1.0.0",
                "core:flow.loop.for:1.0.0",
                "core:placeholder:1.0.0",
                "core:flow.loop.for:1.0.0",
                "core:flow.branch.if:1.0.0",
                "core:flow.branch.switch:1.0.0",
                "core:placeholder:1.0.0"
        );

        Map<String, Object> loopParams = new HashMap<>();
        loopParams.put("index", 0);
        loopParams.put("total", 3);
        loopParams.put("updateExpression", "index + 1");
        loopParams.put("next", List.of("core:placeholder:1.0.0"));
        loopParams.put("loopEnd", List.of("core:flow.branch.if:1.0.0"));

        Map<String, Object> lastOutput = Map.of();
        int executed = 0;

        for (String key : nodeKeys) {
            PluginNodeExecutor executor = registry.getExecutor(PluginNodeIdentifier.fromString(key)).orElseThrow();
            WorkflowConfig config;
            switch (key) {
                case "core:flow.loop.for:1.0.0" -> config = new WorkflowConfig(new HashMap<>(loopParams));
                case "core:placeholder:1.0.0" -> config = new WorkflowConfig(Map.of("index", loopParams.get("index")));
                case "core:flow.branch.if:1.0.0" -> config = new WorkflowConfig(Map.of(
                        "condition", "index == 3",
                        "next_true", List.of("core:flow.branch.switch:1.0.0"),
                        "next_false", List.of("core:placeholder:1.0.0")
                ));
                case "core:flow.branch.switch:1.0.0" -> {
                    List<SwitchCase> cases = List.of(new SwitchCase("match", List.of("core:placeholder:1.0.0")));
                    config = new WorkflowConfig(Map.of(
                            "expression", "match",
                            "cases", cases,
                            "default_case", "core:placeholder:1.0.0"
                    ));
                }
                default -> config = new WorkflowConfig(Map.of());
            }
            var result = executor.execute(config, context);
            switch (key) {
                case "core:flow.loop.for:1.0.0" -> {
                    assertTrue(result.getStatus() == ExecutionStatus.LOOP_NEXT || result.getStatus() == ExecutionStatus.LOOP_END);
                    loopParams.put("index", result.getOutput().get("index"));
                }
                case "core:placeholder:1.0.0" -> assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
                default -> assertEquals(ExecutionStatus.NEXT, result.getStatus());
            }
            lastOutput = result.getOutput();
            executed++;
        }

        assertEquals(10, executed);
        assertEquals(3, lastOutput.get("index"));
    }
}

