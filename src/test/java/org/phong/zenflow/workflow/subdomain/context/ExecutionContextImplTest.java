package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExecutionContextImplTest {

    private ExecutionContextImpl context;

    @BeforeEach
    void setUp() {
        RuntimeContextManager manager = new RuntimeContextManager();
        context = ExecutionContextImpl.builder()
                .workflowId(UUID.randomUUID())
                .workflowRunId(UUID.randomUUID())
                .traceId("trace")
                .userId(UUID.randomUUID())
                .contextManager(manager)
                .templateService(null)
                .logPublisher(null)
                .build();
        context.setNodeKey("http_request");
    }

    @Test
    void readFallsBackToConfigInput() {
        context.setCurrentConfig(new WorkflowConfig(Map.of("url", "https://example.com")));

        String url = context.read("url", String.class);
        assertEquals("https://example.com", url);
    }

    @Test
    void readSupportsNestedConfigPaths() {
        context.setCurrentConfig(new WorkflowConfig(Map.of(
                "request", Map.of(
                        "headers", Map.of("Authorization", "Bearer token")
                )
        )));

        String auth = context.read("request.headers.Authorization", String.class);
        assertEquals("Bearer token", auth);
    }

    @Test
    void readReturnsNullWhenConfigMissing() {
        context.setCurrentConfig(new WorkflowConfig(Map.of()));

        assertNull(context.read("missing", String.class));
    }
}
