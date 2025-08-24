package org.phong.zenflow.workflow.subdomain.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phong.zenflow.workflow.subdomain.logging.core.*;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoggingNodeTest {
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final UUID workflowId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLogContextHierarchyTracking() {
        UUID runId = UUID.randomUUID();
        LogContextManager.init(runId.toString(), "trace-hierarchy");

        NodeLogPublisher publisher = NodeLogPublisher.builder()
            .publisher(eventPublisher)
            .workflowId(workflowId)
            .runId(runId)
            .nodeKey("HierarchyNode")
            .userId(userId)
            .build();

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);

        LogContextManager.push("ComponentA");
        publisher.info("processing");
        LogContextManager.pop();

        verify(eventPublisher, timeout(1000)).publishEvent(captor.capture());
        LogEntry entry = captor.getValue();

        assertEquals("trace-hierarchy", entry.getTraceId());
        assertEquals("ComponentA", entry.getHierarchy());
    }

    @Test
    void testLogMetadataAndCorrelation() {
        UUID runId = UUID.randomUUID();
        LogContextManager.init(runId.toString(), "trace-meta");

        NodeLogPublisher publisher = NodeLogPublisher.builder()
            .publisher(eventPublisher)
            .workflowId(workflowId)
            .runId(runId)
            .nodeKey("MetaNode")
            .userId(userId)
            .build();

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);

        LogContextManager.push("ComponentB");
        publisher.withMeta(Map.of("key", "value")).error("boom");
        LogContextManager.pop();

        verify(eventPublisher, timeout(1000)).publishEvent(captor.capture());
        LogEntry entry = captor.getValue();

        assertEquals(LogLevel.ERROR, entry.getLevel());
        assertEquals("value", entry.getMeta().get("key"));
        assertEquals("ComponentB", entry.getHierarchy());
    }
}
