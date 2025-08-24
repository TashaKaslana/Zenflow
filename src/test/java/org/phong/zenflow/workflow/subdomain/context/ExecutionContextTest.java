package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionContextTest {

    @Test
    void readWriteAndRemove() {
        RuntimeContextManager manager = new RuntimeContextManager();
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        manager.assign(runId.toString(), new RuntimeContext());
        ExecutionContext ctx = ExecutionContext.builder()
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
                .build();

        ctx.write("foo", "bar");
        assertEquals("bar", ctx.read("foo", String.class));
        ctx.remove("foo");
        assertNull(ctx.read("foo", String.class));
    }
}
