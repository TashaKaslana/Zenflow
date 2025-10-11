package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.RequiredArgsConstructor;

@Component
@Order(2)
@RequiredArgsConstructor
public class TimeoutPolicyHandler implements ResiliencePolicyHandler {

    @Qualifier("virtualThreadExecutor")
    private final Executor asyncExecutor;

    @Override
    public Callable<ExecutionResult> apply(Callable<ExecutionResult> callable,
                                           ResolvedExecutionPolicy policy,
                                           String policyKey,
                                           ExecutionTaskEnvelope envelope) {
        if (!policy.hasTimeout()) {
            return callable;
        }

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(policy.getTimeout())
                .cancelRunningFuture(true)
                .build();
        TimeLimiter timeLimiter = TimeLimiter.of(policyKey + "-timeout", timeLimiterConfig);
        return () -> executeWithTimeout(timeLimiter, callable, envelope);
    }

    private ExecutionResult executeWithTimeout(TimeLimiter timeLimiter,
                                               Callable<ExecutionResult> callable,
                                               ExecutionTaskEnvelope envelope) throws Exception {
        Callable<ExecutionResult> timedCallable = TimeLimiter.decorateFutureSupplier(timeLimiter, () ->
                CompletableFuture.supplyAsync(() -> {
                    Thread previous = envelope.currentThread();
                    envelope.attachThread(Thread.currentThread());
                    try {
                        return callInner(callable);
                    } finally {
                        envelope.restoreThread(previous);
                    }
                }, asyncExecutor)
        );
        try {
            return timedCallable.call();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException timeoutException) {
                throw timeoutException;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        }
    }

    private ExecutionResult callInner(Callable<ExecutionResult> callable) {
        try {
            return callable.call();
        } catch (RuntimeException runtimeException) {
            throw runtimeException;
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }
}
