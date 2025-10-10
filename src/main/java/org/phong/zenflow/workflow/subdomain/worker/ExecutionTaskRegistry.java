package org.phong.zenflow.workflow.subdomain.worker;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
public class ExecutionTaskRegistry {
    private final ConcurrentHashMap<String, ExecutionTaskEntry> taskRegistry = new ConcurrentHashMap<>();

    public boolean registerTask(ExecutionTaskEnvelope envelope,
                                CompletableFuture<ExecutionResult> task) {
        if (envelope == null || task == null) {
            throw new IllegalArgumentException("Envelope and task cannot be null");
        }
        return taskRegistry.putIfAbsent(envelope.getTaskId(), new ExecutionTaskEntry(task, envelope)) == null;
    }

    public boolean cancelTask(String taskId) {
        ExecutionTaskEntry entry = taskRegistry.remove(taskId);
        if (entry == null) {
            return false;
        }

        entry.envelope().interrupt();
        return entry.future().cancel(true);
    }

    public void unregisterTask(String taskId) {
        taskRegistry.remove(taskId);
    }

    private record ExecutionTaskEntry(CompletableFuture<ExecutionResult> future,
                                      ExecutionTaskEnvelope envelope) {
    }
}
