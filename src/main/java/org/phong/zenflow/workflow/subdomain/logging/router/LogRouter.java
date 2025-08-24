package org.phong.zenflow.workflow.subdomain.logging.router;
import org.phong.zenflow.workflow.subdomain.logging.core.LogLevel;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.buffer.WorkflowBufferManager;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher.WebSocketNotifier;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogRouter {
    // Bounded queue to decouple hot path from sinks
    private static final int CAPACITY = 100_000;
    private static final BlockingQueue<LogEntry> QUEUE = new ArrayBlockingQueue<>(CAPACITY);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private static volatile WebSocketNotifier ws;
    private static volatile WorkflowBufferManager buffers;
    private static volatile MetricsListener metrics; // optional

    public static void init(WebSocketNotifier wsNotifier, WorkflowBufferManager bufferMgr, MetricsListener metricsListener, int workers){
        LogRouter.ws = wsNotifier; LogRouter.buffers = bufferMgr; LogRouter.metrics = metricsListener;
        if(STARTED.compareAndSet(false,true)){
            ExecutorService pool = Executors.newFixedThreadPool(workers, r -> {
                Thread t = new Thread(r, "log-router"); t.setDaemon(true); return t;
            });
            for(int i=0;i<workers;i++){
                pool.submit(() -> {
                    try {
                        while(!Thread.currentThread().isInterrupted()){
                            LogEntry e = QUEUE.take();
                            // FAST lane: immediate WebSocket
                            if(ws != null) ws.onLog(e);
                            // DURABLE lane: enqueue to per-run buffer
                            if(buffers != null) buffers.enqueue(e);
                            if(metrics != null) metrics.onLog(e);
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); // Restore interrupt status
                    }
                });
            }
        }
    }

    public static void dispatch(LogEntry entry){
        // Drop oldest DEBUG if overloaded (simple backpressure policy)
        if(!QUEUE.offer(entry)){
            if(entry.getLevel() == LogLevel.DEBUG) return;
            QUEUE.poll(); // free one
            if(!QUEUE.offer(entry)) {
                // Log entry could not be queued even after making space
                // This is a rare edge case but we should handle it gracefully
                System.err.println("Warning: Failed to enqueue log entry after retry");
            }
        }
    }

    public interface MetricsListener { void onLog(LogEntry e); }
}
