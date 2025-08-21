package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;// logging/buffer/WorkflowBufferManager.java
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;

import java.util.*;
import java.util.concurrent.*;

public class WorkflowBufferManager {
    private final ConcurrentMap<UUID, WorkflowBuffer> buffers = new ConcurrentHashMap<>();
    private final GlobalLogCollector collector;
    private final int batchSize;
    private final long maxDelayMillis;
    private final int ringSize;

    public WorkflowBufferManager(GlobalLogCollector collector, int batchSize, long maxDelayMillis, int ringSize){
        this.collector = collector; this.batchSize = batchSize; this.maxDelayMillis = maxDelayMillis; this.ringSize = ringSize;
    }

    public void startRun(UUID runId){
        buffers.computeIfAbsent(runId, id -> new WorkflowBuffer(id, collector, batchSize, maxDelayMillis, ringSize));
    }

    public void endRun(UUID runId){
        WorkflowBuffer buf = buffers.remove(runId);
        if(buf != null) buf.closeAndFlush();
    }

    public void enqueue(LogEntry e){
        // Ensure buffer exists (idempotent)
        startRun(e.getWorkflowRunId());
        buffers.get(e.getWorkflowRunId()).append(e);
    }

    public List<LogEntry> recent(UUID runId, int limit){
        WorkflowBuffer buf = buffers.get(runId);
        return buf == null ? List.of() : buf.recent(limit);
    }
}
