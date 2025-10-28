package org.phong.zenflow;

import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextImpl;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.context.common.ContextKeyResolver;
import org.phong.zenflow.workflow.subdomain.context.resolution.SystemLoadMonitor;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.string.StringContainsFunction;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;

public class TestExecutionContextUtils {
    private static UUID runId = UUID.randomUUID();
    private static UUID workflowId = UUID.randomUUID();
    private static RuntimeContextManager sharedContextManager = new RuntimeContextManager();
    
    public static ExecutionContext createExecutionContext() {
        return createExecutionContext(event -> {});
    }

    public static ExecutionContext createExecutionContext(ApplicationEventPublisher publisher) {
        RuntimeContext runtimeContext = new RuntimeContext();
        sharedContextManager.assign(runId.toString(), runtimeContext);
        ContextValueResolver resolver = new ContextValueResolver(new SystemLoadMonitor());
        ExecutionContext context = ExecutionContextImpl.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .traceId("trace")
                .userId(null)
                .contextManager(sharedContextManager)
                .logPublisher(NodeLogPublisher.builder()
                        .publisher(publisher)
                        .workflowId(workflowId)
                        .runId(runId)
                        .userId(null)
                        .build())
                .templateService(new TemplateService(new AviatorFunctionRegistry(List.of(new StringContainsFunction()))))
                .contextValueResolver(resolver)
                .build();
        context.setNodeKey("test-node");
        return context;
    }

    public static ExecutionContext createExecutionContext(WorkflowConfig config) {
        ExecutionContext context = createExecutionContext();
        context.setCurrentConfig(config);
        return context;
    }

    /**
     * Flushes pending writes for testing purposes.
     * In production, WorkflowEngineService handles this.
     * <p>
     * NOTE: This registers a fake consumer for all pending writes to satisfy the
     * selective storage check, then flushes and cleans up. This allows tests to
     * verify outputs without modifying production code.
     */
    public static void flushPendingWrites(ExecutionContext context) {
        if (context == null || context.getWorkflowRunId() == null) {
            throw new IllegalArgumentException("Context or WorkflowRunId cannot be null");
        }

        RuntimeContext runtimeContext = sharedContextManager.getOrCreate(context.getWorkflowRunId().toString());
        
        // Get all pending write keys and register a fake consumer for each
        // This satisfies the selective storage check without forcing storage
        Map<String, Object> pendingWrites = runtimeContext.getPendingWrites();
        Map<String, Set<String>> fakeConsumers = new HashMap<>();
        for (String key : pendingWrites.keySet()) {
            String scopedKey = ContextKeyResolver.scopeKey(
                context.getNodeKey(), key
            );
            fakeConsumers.put(scopedKey, Set.of("__test_consumer__"));
        }
        
        // Register the fake consumers
        runtimeContext.initialize(null, fakeConsumers, null);
        
        // Now flush will store the values since they have consumers
        runtimeContext.flushPendingWrites(context.getNodeKey());
    }
    
    public static RuntimeContext getCurrentRuntimeContext() {
        return sharedContextManager.getOrCreate(runId.toString());
    }
}

