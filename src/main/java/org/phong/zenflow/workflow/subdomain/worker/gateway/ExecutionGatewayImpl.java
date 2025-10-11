package org.phong.zenflow.workflow.subdomain.worker.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.services.NodeExecutorDispatcher;
import org.phong.zenflow.workflow.subdomain.worker.ExecutionTaskRegistry;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
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
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionTaskEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("Execution envelope must not be null");
        }
        if (envelope.getExecutorIdentifier() == null || envelope.getExecutorIdentifier().isBlank()) {
            throw new IllegalStateException("Missing executor identifier in envelope");
        }
        if (envelope.getExecutorType() == null || envelope.getExecutorType().isBlank()) {
            throw new IllegalStateException("Missing executor type in envelope");
        }
        if (envelope.getConfig() == null) {
            throw new IllegalStateException("Missing resolved workflow configuration in envelope");
        }

        CompletableFuture<ExecutionResult> future = new CompletableFuture<>();
        if (!registry.registerTask(envelope, future)) {
            IllegalStateException ex = new IllegalStateException("Task already registered: " + envelope.getTaskId() + ", cannot execute");
            log.error("Failed to register execution task {}", envelope.getTaskId(), ex);
            future.completeExceptionally(ex);
            return future;
        }

        taskExecutor.execute(() -> runTask(envelope, future));
        return future;
    }

    private void runTask(ExecutionTaskEnvelope envelope,
                         CompletableFuture<ExecutionResult> future) {
        Thread previousThread = envelope.currentThread();
        envelope.attachThread(Thread.currentThread());
        try {
            if (future.isCancelled()) {
                completeCancelled(future);
                return;
            }

            ExecutionResult result = nodeExecutorDispatcher.dispatch(envelope);
            future.complete(result);
        } catch (Throwable throwable) {
            if (isInterrupted(throwable)) {
                Thread.currentThread().interrupt();
                log.debug("Execution task {} interrupted", envelope.getTaskId(), throwable);
                future.complete(ExecutionResult.interruptedResult("Execution interrupted"));
            } else {
                log.warn("Asynchronous execution task {} failed", envelope.getTaskId(), throwable);
                future.complete(ExecutionResult.error(ExecutionError.NON_RETRIABLE, throwable.getMessage()));
            }
        } finally {
            envelope.restoreThread(previousThread);
            registry.unregisterTask(envelope.getTaskId());
        }
    }

    private void completeCancelled(CompletableFuture<ExecutionResult> future) {
        if (!future.isDone()) {
            future.complete(ExecutionResult.cancelledResult("Execution cancelled"));
        }
    }

    private boolean isInterrupted(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InterruptedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Override
    public boolean cancelAsync(ExecutionTaskEnvelope envelope) {
        return registry.cancelTask(envelope.getTaskId());
    }
}
