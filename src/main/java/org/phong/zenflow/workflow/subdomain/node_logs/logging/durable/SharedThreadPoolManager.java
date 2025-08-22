package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SharedThreadPoolManager {

    @Getter
    private final ScheduledExecutorService sharedScheduler;
    @Getter
    private final ExecutorService batchProcessorService;
    private final AtomicInteger activeWorkflows = new AtomicInteger(0);

    public SharedThreadPoolManager(LoggingProperties properties) {
        LoggingProperties.ThreadPoolConfig config = properties.getThreadPool();

        // Shared scheduler for all workflow buffers
        this.sharedScheduler = Executors.newScheduledThreadPool(
            config.getCorePoolSize(),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "logging-scheduler-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            }
        );

        // Batch processor service for handling flush operations
        this.batchProcessorService = new ThreadPoolExecutor(
            config.getCorePoolSize(),
            config.getMaximumPoolSize(),
            config.getKeepAliveTimeMs(),
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(config.getQueueCapacity()),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "logging-batch-processor-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure handling
        );
    }

    public void incrementActiveWorkflows() {
        activeWorkflows.incrementAndGet();
    }

    public void decrementActiveWorkflows() {
        activeWorkflows.decrementAndGet();
    }

    public int getActiveWorkflowCount() {
        return activeWorkflows.get();
    }

    public void shutdown() {
        sharedScheduler.shutdown();
        batchProcessorService.shutdown();

        try {
            if (!sharedScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                sharedScheduler.shutdownNow();
            }
            if (!batchProcessorService.awaitTermination(30, TimeUnit.SECONDS)) {
                batchProcessorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sharedScheduler.shutdownNow();
            batchProcessorService.shutdownNow();
        }
    }
}
