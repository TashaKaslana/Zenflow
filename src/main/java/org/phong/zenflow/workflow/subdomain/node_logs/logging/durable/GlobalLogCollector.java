package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;// logging/collector/GlobalLogCollector.java
import org.phong.zenflow.workflow.subdomain.node_logs.enums.LogLevel;
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;

import java.util.*;
import java.util.concurrent.*;

public class GlobalLogCollector {
    private static final int CAPACITY = 10_000;
    private final BlockingQueue<Batch> queue = new ArrayBlockingQueue<>(CAPACITY);
    private final PersistenceService persistence;   // DB writer (batch)
    private final KafkaPublisher kafka;             // optional; can be null

    public GlobalLogCollector(PersistenceService persistence, KafkaPublisher kafka, int workers){
        this.persistence = persistence; this.kafka = kafka;
        ExecutorService pool = Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "log-collector"); t.setDaemon(true); return t;
        });
        for(int i=0;i<workers;i++){
            pool.submit(() -> {
                while(true){
                    Batch b = queue.take();
                    try {
                        // 1) persist to DB
                        persistence.saveBatch(b.runId, b.entries);
                        // 2) forward to Kafka (optional), partition by runId to keep order
                        if(kafka != null) kafka.publish(b.entries);
                    } catch (Exception ex) {
                        // Simple retry/backoff; in prod consider DLQ/spool
                        TimeUnit.MILLISECONDS.sleep(200);
                        queue.offer(b);
                    }
                }
            });
        }
    }

    public void accept(UUID runId, List<LogEntry> entries){
        if(entries == null || entries.isEmpty()) return;
        if(!queue.offer(new Batch(runId, entries))){
            // backpressure policy: drop DEBUG batches first, or split + retry
            // here: best effort re-offer after removing some DEBUGs
            List<LogEntry> pruned = entries.stream()
                .filter(e -> e.getLevel() != LogLevel.DEBUG).toList();
            if(!pruned.isEmpty()) queue.offer(new Batch(runId, pruned));
        }
    }

    private record Batch(UUID runId, List<LogEntry> entries) {}
}
