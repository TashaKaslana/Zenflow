package org.phong.zenflow.workflow.subdomain.engine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.if_node.IfNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node.SwitchCase;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.branch.switch_node.SwitchNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.flow.loop.for_loop.ForLoopExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder.PlaceholderExecutor;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.string.StringContainsFunction;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.phong.zenflow.workflow.subdomain.context.ReadOptions.PREFER_CONTEXT;

@SpringJUnitConfig(classes = {
        ForLoopExecutor.class,
        IfNodeExecutor.class,
        SwitchNodeExecutor.class,
        PlaceholderExecutor.class,
        TemplateService.class,
        PluginNodeExecutorRegistry.class,
        AviatorFunctionRegistry.class,
        StringContainsFunction.class
})
class WorkflowExecutionSampleTest {

    @Autowired
    private PluginNodeExecutorRegistry registry;

    @Autowired
    private ApplicationContext applicationContext;

    String loopKey = "loop";
    String placeholderKey = "placeholder";
    String ifKey = "if";
    String switchKey = "switch";

    @BeforeEach
    void setUp() {
        // Register all executors with both UUIDs and composite keys
        // This simulates what the PluginNodeSynchronizer would do when processing @PluginNode annotations

        ForLoopExecutor forLoopExecutor = applicationContext.getBean(ForLoopExecutor.class);
        // Define UUIDs for each executor (following the UUID pattern used elsewhere)
        String forLoopUuid = "223e4567-e89b-12d3-a456-426614174003";
        NodeDefinition forLoopDefinition = NodeDefinition.builder()
                .nodeExecutor(forLoopExecutor)
                .build();
        registry.register(forLoopUuid, () -> forLoopDefinition);
        registry.register("core:flow.loop.for:1.0.0", () -> forLoopDefinition);

        IfNodeExecutor ifNodeExecutor = applicationContext.getBean(IfNodeExecutor.class);
        NodeDefinition ifDefinition = NodeDefinition.builder()
                .nodeExecutor(ifNodeExecutor)
                .build();
        String ifNodeUuid = "323e4567-e89b-12d3-a456-426614174004";
        registry.register(ifNodeUuid, () -> ifDefinition);
        registry.register("core:flow.branch.if:1.0.0", () -> ifDefinition);

        SwitchNodeExecutor switchNodeExecutor = applicationContext.getBean(SwitchNodeExecutor.class);
        NodeDefinition switchDefinition = NodeDefinition.builder()
                .nodeExecutor(switchNodeExecutor)
                .build();
        String switchNodeUuid = "423e4567-e89b-12d3-a456-426614174005";
        registry.register(switchNodeUuid, () -> switchDefinition);
        registry.register("core:flow.branch.switch:1.0.0", () -> switchDefinition);

        PlaceholderExecutor placeholderExecutor = applicationContext.getBean(PlaceholderExecutor.class);
        NodeDefinition placeholderDefinition = NodeDefinition.builder()
                .nodeExecutor(placeholderExecutor)
                .build();
        // Same as other tests
        String placeholderUuid = "123e4567-e89b-12d3-a456-426614174001";
        registry.register(placeholderUuid, () -> placeholderDefinition);
        registry.register("test:placeholder:1.0.0", () -> placeholderDefinition);
    }

    @Test
    void executesWorkflowWithLoopAndBranches() {
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();
        List<String> nodeKeys = List.of(
                "core:flow.loop.for:1.0.0",
                "test:placeholder:1.0.0",
                "core:flow.loop.for:1.0.0",
                "test:placeholder:1.0.0",
                "core:flow.loop.for:1.0.0",
                "test:placeholder:1.0.0",
                "core:flow.loop.for:1.0.0",
                "core:flow.branch.if:1.0.0",
                "core:flow.branch.switch:1.0.0",
                "test:placeholder:1.0.0"
        );


        Map<String, Object> loopParams = new HashMap<>();
        loopParams.put("index", 0);
        loopParams.put("total", 3);
        loopParams.put("updateExpression", "index + 1");
        loopParams.put("next", List.of("test:placeholder:1.0.0"));
        loopParams.put("loopEnd", List.of("core:flow.branch.if:1.0.0"));

        int executed = 0;
        boolean inLoop = false;
        int indexCountResult = -1;
        RuntimeContext runtimeContext = TestExecutionContextUtils.getCurrentRuntimeContext();

        for (String key : nodeKeys) {
            // Use the key directly as it should now be a UUID string after our refactoring
            // If we have composite keys, we need to handle the conversion properly
            NodeDefinition definition = registry.getDefinition(key).orElseThrow();
            NodeExecutor executor = definition.getNodeExecutor();
            
            // Set node key for proper scoping
            String nodeKey = getNodeKey(key);
            context.setNodeKey(nodeKey);
            
            // Start loop if this is a loop node
            if (loopKey.equals(nodeKey) && !inLoop) {
                runtimeContext.startLoop(loopKey);
                inLoop = true;
            }
            
            WorkflowConfig config;
            switch (key) {
                case "core:flow.loop.for:1.0.0" -> config = new WorkflowConfig(new HashMap<>(loopParams), Map.of());
                case "test:placeholder:1.0.0" -> {
                    // Read current index from context for placeholder node
                    Integer currentIndex = context.read("index", Integer.class);
                    config = new WorkflowConfig(Map.of("index", currentIndex != null ? currentIndex : 0), Map.of());
                }
                case "core:flow.branch.if:1.0.0" -> config = new WorkflowConfig(Map.of(
                        "condition", "index == 3",
                        "next_true", List.of("core:flow.branch.switch:1.0.0"),
                        "next_false", List.of("test:placeholder:1.0.0")
                ), Map.of());
                case "core:flow.branch.switch:1.0.0" -> {
                    List<SwitchCase> cases = List.of(new SwitchCase("match", List.of("test:placeholder:1.0.0")));
                    config = new WorkflowConfig(Map.of(
                            "expression", "match",
                            "cases", cases,
                            "default_case", "test:placeholder:1.0.0"
                    ), Map.of());
                }
                default -> config = new WorkflowConfig(Map.of(), Map.of());
            }
            ExecutionResult result;
            try {
                context.setCurrentConfig(config);
                result = executor.execute(context);
                TestExecutionContextUtils.flushPendingWrites(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            switch (key) {
                case "core:flow.loop.for:1.0.0" -> {
                    assertTrue(result.getStatus() == ExecutionStatus.LOOP_NEXT || result.getStatus() == ExecutionStatus.LOOP_END);
                    // End loop when we get LOOP_END status
                    if (result.getStatus() == ExecutionStatus.LOOP_END) {
                        indexCountResult = context.read("index", Integer.class, PREFER_CONTEXT);
                        runtimeContext.endLoop(loopKey);
                        inLoop = false;
                    }
                }
                case "test:placeholder:1.0.0" -> assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
                default -> assertEquals(ExecutionStatus.NEXT, result.getStatus());
            }
            executed++;
        }

        assertEquals(10, executed);
        // Check final index from the last loop executor's scoped context
        context.setNodeKey(getNodeKey("core:flow.loop.for:1.0.0"));
        assertEquals(3, indexCountResult);
    }

    private String getNodeKey(String executorKey) {
        return switch (executorKey) {
            case "core:flow.branch.if:1.0.0" -> ifKey;
            case "core:flow.branch.switch:1.0.0" -> switchKey;
            case "core:flow.loop.for:1.0.0" -> loopKey;
            case "test:placeholder:1.0.0" -> placeholderKey;

            default -> null;
        };
    }
}
