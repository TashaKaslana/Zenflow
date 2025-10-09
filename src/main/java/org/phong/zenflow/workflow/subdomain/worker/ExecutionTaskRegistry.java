package org.phong.zenflow.workflow.subdomain.worker;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
public class ExecutionTaskRegistry {
    private final ConcurrentHashMap<String, ExecutionTaskEntry> taskRegistry = new ConcurrentHashMap<>();

    public boolean registerTask(String taskId,
                                CompletableFuture<ExecutionResult> task,
                                ExecutionContext context) {
        if (taskId == null || task == null || context == null) {
            throw new IllegalArgumentException("Task ID, task, and context cannot be null");
        }
        return taskRegistry.putIfAbsent(taskId, new ExecutionTaskEntry(task, context)) == null;
    }

    public CompletableFuture<ExecutionResult> getTask(String taskId) {
        ExecutionTaskEntry entry = taskRegistry.get(taskId);
        return entry != null ? entry.future() : null;
    }

    public boolean cancelTask(String taskId) {
        ExecutionTaskEntry entry = taskRegistry.remove(taskId);
        if (entry == null) {
            return false;
        }

        Thread executionThread = entry.context().getExecutionThread();
        if (executionThread != null) {
            executionThread.interrupt();
        }
        return entry.future().cancel(true);
    }

    public void unregisterTask(String taskId) {
        taskRegistry.remove(taskId);
    }

    private record ExecutionTaskEntry(CompletableFuture<ExecutionResult> future,
                                      ExecutionContext context) {
    }
}
