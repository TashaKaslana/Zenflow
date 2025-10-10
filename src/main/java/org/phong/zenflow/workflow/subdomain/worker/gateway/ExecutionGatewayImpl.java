package org.phong.zenflow.workflow.subdomain.worker.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.services.NodeExecutorDispatcher;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.worker.ExecutionTaskRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionGatewayImpl implements ExecutionGateway {
    private final ExecutionTaskRegistry registry;
    private final NodeExecutorDispatcher nodeExecutorDispatcher;
    @Qualifier("virtualThreadExecutor")
    private final Executor taskExecutor;

    @Override
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionContext context) {
        if (context.getPluginNodeId() == null) {
            throw new IllegalStateException("Execution context is missing plugin node identifier");
        }
        if (context.getExecutorType() == null || context.getExecutorType().isBlank()) {
            throw new IllegalStateException("Execution context is missing executor type");
        }
        if (context.getCurrentConfig() == null) {
            throw new IllegalStateException("Execution context is missing resolved workflow configuration");
        }

        String taskId = context.taskId();
        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();

        if (!registry.registerTask(taskId, future, context)) {
            IllegalStateException ex = new IllegalStateException("Task already registered: " + taskId);
            future.completeExceptionally(ex);
            return future;
        }

        taskExecutor.execute(() -> runTask(context, future, taskId));
        return future;
    }

    private void runTask(ExecutionContext context,
                         CompletableFuture<ExecutionResult> future,
                         String taskId) {
        Thread previousThread = context.getExecutionThread();
        context.setExecutionThread(Thread.currentThread());
        try {
            if (future.isCancelled()) {
                completeCancelled(future);
                return;
            }

            ExecutionResult result = nodeExecutorDispatcher.dispatch(
                    context.getPluginNodeId().toString(),
                    context.getExecutorType(),
                    context.getCurrentConfig(),
                    context
            );
            future.complete(result);
        } catch (Throwable throwable) {
            log.warn("Asynchronous execution task {} failed", taskId, throwable);
            future.complete(ExecutionResult.error(ExecutionError.NON_RETRIABLE, throwable.getMessage()));
        } finally {
            context.setExecutionThread(previousThread);
            registry.unregisterTask(taskId);
        }
    }

    private void completeCancelled(CompletableFuture<ExecutionResult> future) {
        if (!future.isDone()) {
            future.complete(ExecutionResult.cancelledResult("Execution cancelled"));
        }
    }

    @Override
    public boolean cancelAsync(ExecutionContext context) {
        return registry.cancelTask(context.taskId());
    }
}
