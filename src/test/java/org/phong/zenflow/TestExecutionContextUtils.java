package org.phong.zenflow;

import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

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
        return ExecutionContext.builder()
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
                .build();
    }
}
