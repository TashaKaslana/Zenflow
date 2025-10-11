package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.string.StringContainsFunction;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionContextTest {

    @Test
    void readWriteAndRemove() {
        RuntimeContextManager manager = new RuntimeContextManager();
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        manager.assign(runId.toString(), new RuntimeContext());
        ExecutionContext ctx = ExecutionContextImpl.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .traceId("trace")
                .userId(null)
                .contextManager(manager)
                .logPublisher(NodeLogPublisher.builder()
                        .publisher(event -> {})
                        .workflowId(workflowId)
                        .runId(runId)
                        .userId(null)
                        .build())
                .templateService(new TemplateService(new AviatorFunctionRegistry(List.of(new StringContainsFunction()))))
                .build();

        ctx.write("foo", "bar");
        assertEquals("bar", ctx.read("foo", String.class));
        ctx.remove("foo");
        assertNull(ctx.read("foo", String.class));
    }

    @Test
    void setNodeKeyPropagatesToLogPublisher() {
        RuntimeContextManager manager = new RuntimeContextManager();
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        manager.assign(runId.toString(), new RuntimeContext());

        final LogEntry[] captured = new LogEntry[1];
        ExecutionContext ctx = ExecutionContextImpl.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .traceId("trace")
                .userId(null)
                .contextManager(manager)
                .logPublisher(NodeLogPublisher.builder()
                        .publisher(event -> captured[0] = (LogEntry) event)
                        .workflowId(workflowId)
                        .runId(runId)
                        .userId(null)
                        .build())
                .templateService(new TemplateService(new AviatorFunctionRegistry(List.of(new StringContainsFunction()))))
                .build();

        ctx.setNodeKey("test-node");
        ctx.getLogPublisher().info("hello");

        assertNotNull(captured[0]);
        assertEquals("test-node", captured[0].getNodeKey());
    }
}
