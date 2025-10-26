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

public class TestExecutionContextUtils {
    public static ExecutionContext createExecutionContext() {
        return createExecutionContext(event -> {});
    }

    public static ExecutionContext createExecutionContext(ApplicationEventPublisher publisher) {
        RuntimeContextManager manager = new RuntimeContextManager();
        RuntimeContext runtimeContext = new RuntimeContext();
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        manager.assign(runId.toString(), runtimeContext);
        ContextValueResolver resolver = new ContextValueResolver(new SystemLoadMonitor());
        ExecutionContext context = ExecutionContextImpl.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .traceId("trace")
                .userId(null)
                .contextManager(manager)
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
}

