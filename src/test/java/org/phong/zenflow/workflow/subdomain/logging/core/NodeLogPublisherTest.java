package org.phong.zenflow.workflow.subdomain.logging.core;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NodeLogPublisherTest {

    private NodeLogPublisher createPublisher(ApplicationEventPublisher publisher) {
        return NodeLogPublisher.builder()
                .publisher(publisher)
                .workflowId(UUID.randomUUID())
                .runId(UUID.randomUUID())
                .nodeKey("node")
                .userId(UUID.randomUUID())
                .build();
    }

    @Test
    void withMetaInfoProducesMetadata() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        NodeLogPublisher log = createPublisher(publisher);

        Map<String, Object> meta = Map.of("a", 1);
        log.withMeta(meta).info("hello");

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(publisher).publishEvent(captor.capture());
        LogEntry entry = captor.getValue();
        assertEquals(meta, entry.getMeta());
        assertEquals(LogLevel.INFO, entry.getLevel());
    }

    @Test
    void withMetaAndExceptionErrorProducesStackTrace() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        NodeLogPublisher log = createPublisher(publisher);

        Map<String, Object> meta = Map.of("b", 2);
        RuntimeException ex = new RuntimeException("boom");
        log.withMeta(meta).withException(ex).error("fail");

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(publisher).publishEvent(captor.capture());
        LogEntry entry = captor.getValue();
        assertEquals(LogLevel.ERROR, entry.getLevel());
        assertEquals("boom", entry.getErrorMessage());
        assertNotNull(entry.getMeta());
        assertEquals(2, entry.getMeta().get("b"));
        assertTrue(entry.getMeta().get("stackTrace").toString().contains("RuntimeException"));
    }
}
