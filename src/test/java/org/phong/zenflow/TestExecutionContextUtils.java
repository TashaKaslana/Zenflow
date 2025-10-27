package org.phong.zenflow;

import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextImpl;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.context.resolution.SystemLoadMonitor;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.string.StringContainsFunction;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

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
     * NOTE: This bypasses selective storage and writes all pending values to context,
     * regardless of whether they have registered consumers. This is necessary for testing
     * loop executors where loop state (index, etc.) needs to persist between iterations.
     */
    public static void flushPendingWrites(ExecutionContext context) {
        if (context == null || context.getWorkflowRunId() == null) {
            throw new IllegalArgumentException("Context or WorkflowRunId cannot be null");
        }

        RuntimeContext candidate = sharedContextManager.getOrCreate(context.getWorkflowRunId().toString());
        RuntimeContext runtimeContext = org.mockito.Mockito.mockingDetails(candidate).isMock()
                ? candidate  // already mock — don't spy again
                : spy(candidate);

        doReturn(false).when(runtimeContext).isConsumersEmpty(anyString());
        sharedContextManager.assign(context.getWorkflowRunId().toString(), runtimeContext);
        runtimeContext.flushPendingWrites(context.getNodeKey());
    }
    
    public static RuntimeContext getCurrentRuntimeContext() {
        return sharedContextManager.getOrCreate(runId.toString());
    }
}

