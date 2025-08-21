package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;// logging/buffer/WorkflowBuffer.java
import org.phong.zenflow.workflow.subdomain.node_logs.enums.LogLevel;
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

class WorkflowBuffer {
    private final UUID runId;
    private final GlobalLogCollector collector;
    private final int batchSize;
    private final long maxDelayMillis;

    // FIFO for batch, lock-free for producers
    private final ConcurrentLinkedQueue<LogEntry> queue = new ConcurrentLinkedQueue<>();
    // Small ring buffer for reconnect/recent view
    private final ArrayDeque<LogEntry> ring;
    private final int ringCap;

    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> ticker;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    WorkflowBuffer(UUID runId, GlobalLogCollector collector, int batchSize, long maxDelayMillis, int ringSize){
        this.runId = runId; this.collector = collector; this.batchSize = batchSize; this.maxDelayMillis = maxDelayMillis;
        this.ringCap = Math.max(1, ringSize); this.ring = new ArrayDeque<>(ringCap);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "wfbuf-"+runId); t.setDaemon(true); return t;
        });
        this.ticker = scheduler.scheduleAtFixedRate(this::flushIfAny, maxDelayMillis, maxDelayMillis, TimeUnit.MILLISECONDS);
    }

    void append(LogEntry e){
        if(closed.get()) return;
        queue.offer(e);
        addToRing(e);
        // immediate flush on errors (include prior)
        if(e.getLevel() == LogLevel.ERROR || queue.size() >= batchSize){
            flushIfAny();
        }
    }

    synchronized void flushIfAny(){
        if(queue.isEmpty()) return;
        List<LogEntry> batch = new ArrayList<>(Math.min(queue.size(), batchSize));
        LogEntry x;
        while((x = queue.poll()) != null){
            batch.add(x);
            if(batch.size() >= batchSize && !queue.isEmpty()){
                collector.accept(runId, batch);
                batch = new ArrayList<>(Math.min(queue.size(), batchSize));
            }
        }
        if(!batch.isEmpty()) collector.accept(runId, batch);
    }

    List<LogEntry> recent(int limit){
        synchronized (ring) {
            return ring.stream().skip(Math.max(0, ring.size()-limit)).toList();
        }
    }

    void closeAndFlush(){
        if(closed.compareAndSet(false,true)){
            try { ticker.cancel(false); } catch (Exception ignore) {}
            flushIfAny();
            scheduler.shutdownNow();
        }
    }

    private void addToRing(LogEntry e){
        synchronized (ring) {
            if(ring.size() == ringCap) ring.removeFirst();
            ring.addLast(e);
        }
    }
}
