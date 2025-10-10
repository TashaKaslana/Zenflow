package org.phong.zenflow.workflow.subdomain.worker.model;

import lombok.Builder;
import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Builder
public class ExecutionTaskEnvelope {
    private final String taskId;
    private final String executorIdentifier;
    private final String executorType;
    private final WorkflowConfig config;
    private final ExecutionContext context;
    private final UUID pluginNodeId;

    @Builder.Default
    private final AtomicReference<Thread> executionThread = new AtomicReference<>();

    public void attachThread(Thread thread) {
        executionThread.set(thread);
    }

    public void restoreThread(Thread thread) {
        executionThread.set(thread);
    }

    public Thread currentThread() {
        return executionThread.get();
    }

    public void clearThread() {
        executionThread.set(null);
    }

    public void interrupt() {
        Thread thread = executionThread.get();
        if (thread != null) {
            thread.interrupt();
        }
    }
}
