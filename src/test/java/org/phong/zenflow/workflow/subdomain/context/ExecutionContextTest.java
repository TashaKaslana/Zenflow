package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.context.resolution.SystemLoadMonitor;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.string.StringContainsFunction;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionContextTest {

    @Test
    void readWriteAndRemove() {
        RuntimeContextManager manager = new RuntimeContextManager();
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        String nodeKey = "node";
        RuntimeContext runtimeContext = new RuntimeContext();
        manager.assign(runId.toString(), runtimeContext);
        runtimeContext.initialize(null, Map.of(
                "node.output.foo", Set.of("node1"),
                "node.output.test", Set.of("node1")
        ), null);
        ContextValueResolver resolver = new ContextValueResolver(new SystemLoadMonitor());
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
                .contextValueResolver(resolver)
                .nodeKey(nodeKey)
                .build();

        // Test write/read/remove pattern (as used by WorkflowEngineService)
        ctx.write("foo", "bar");
        assertNull(ctx.read("foo", String.class)); // Not visible yet
        runtimeContext.flushPendingWrites(nodeKey); // Simulate what WorkflowEngineService does
        assertEquals("bar", ctx.read("foo", String.class)); // Now visible
        ctx.remove("foo");
        assertNull(ctx.read("foo", String.class));
        
        // Test another write/flush cycle
        ctx.write("test", "value");
        assertNull(ctx.read("test", String.class)); // Not visible yet
        runtimeContext.flushPendingWrites(nodeKey); // Simulate what WorkflowEngineService does
        assertEquals("value", ctx.read("test", String.class)); // Now visible
    }

    @Test
    void setNodeKeyPropagatesToLogPublisher() {
        RuntimeContextManager manager = new RuntimeContextManager();
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        manager.assign(runId.toString(), new RuntimeContext());
        ContextValueResolver resolver = new ContextValueResolver(new SystemLoadMonitor());

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
                .contextValueResolver(resolver)
                .build();

        ctx.setNodeKey("test-node");
        ctx.getLogPublisher().info("hello");

        assertNotNull(captured[0]);
        assertEquals("test-node", captured[0].getNodeKey());
    }
}
