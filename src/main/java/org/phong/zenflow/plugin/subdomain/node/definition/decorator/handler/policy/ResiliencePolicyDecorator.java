package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.worker.policy.ExecutionPolicyResolver;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.util.concurrent.TimeoutException;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResiliencePolicyDecorator implements ExecutorDecorator {

    private final ExecutionPolicyResolver policyResolver;
    @Qualifier("virtualThreadExecutor")
    private final Executor asyncExecutor;

    @Override
    public int order() {
        return 500;
    }

    @Override
    public Callable<ExecutionResult> decorate(Callable<ExecutionResult> inner,
                                              NodeDefinition def,
                                              WorkflowConfig cfg,
                                              ExecutionContext ctx) {
        ResolvedExecutionPolicy policy = policyResolver.resolve(def, cfg);
        if (policy.isEmpty()) {
            return inner;
        }

        Callable<ExecutionResult> wrapped = inner;
        String policyKey = resolvePolicyKey(ctx, def);

        if (policy.hasRateLimit()) {
            RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                    .limitForPeriod(policy.getRateLimit().getLimitForPeriod())
                    .limitRefreshPeriod(policy.getRateLimit().getRefreshPeriodOrDefault())
                    .timeoutDuration(policy.getRateLimit().getTimeoutDurationOrDefault())
                    .build();
            RateLimiter rateLimiter = RateLimiter.of(policyKey + "-rate", rateLimiterConfig);
            wrapped = RateLimiter.decorateCallable(rateLimiter, wrapped);
        }

        if (policy.hasRetry()) {
            RetryConfig.Builder<ExecutionResult> retryBuilder = RetryConfig.<ExecutionResult>custom()
                    .maxAttempts(policy.getRetry().getMaxAttempts())
                    .retryExceptions(Exception.class);

            Duration waitDuration = policy.getRetry().getWaitDuration();
            if (waitDuration != null && !waitDuration.isNegative()) {
                retryBuilder.waitDuration(waitDuration);
            }

            // treat explicit RETRY status as failure to trigger retry
            retryBuilder.retryOnResult(result -> result != null && result.getStatus() == ExecutionStatus.RETRY);

            Retry retry = Retry.of(policyKey + "-retry", retryBuilder.build());
            wrapped = Retry.decorateCallable(retry, wrapped);
        }

        if (policy.hasTimeout()) {
            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(policy.getTimeout())
                    .cancelRunningFuture(true)
                    .build();
            TimeLimiter timeLimiter = TimeLimiter.of(policyKey + "-timeout", timeLimiterConfig);
            Callable<ExecutionResult> delegate = wrapped;
            wrapped = () -> executeWithTimeout(timeLimiter, delegate, ctx);
        }

        Callable<ExecutionResult> finalWrapped = wrapped;
        return () -> {
            try {
                return finalWrapped.call();
            } catch (TimeoutException e) {
                String message = "Execution timed out after " + policy.getTimeout();
                log.warn("[policyKey={}] {}", policyKey, message);
                return ExecutionResult.error(ExecutionError.TIMEOUT, message);
            } catch (RequestNotPermitted e) {
                String message = "Rate limit exceeded for node execution";
                log.warn("[policyKey={}] {}", policyKey, message);
                return ExecutionResult.error(ExecutionError.RETRIABLE, message);
            } catch (MaxRetriesExceededException e) {
                String message = "Maximum retry attempts exhausted";
                log.warn("[policyKey={}] {}", policyKey, message, e);
                return ExecutionResult.error(ExecutionError.RETRIABLE, message);
            } catch (CompletionException e) {
                Throwable cause = Optional.ofNullable(e.getCause()).orElse(e);
                if (cause instanceof TimeoutException timeoutException) {
                    String message = "Execution timed out after " + policy.getTimeout();
                    log.warn("[policyKey={}] {}", policyKey, message);
                    return ExecutionResult.error(ExecutionError.TIMEOUT, message);
                }
                if (cause instanceof RequestNotPermitted requestNotPermitted) {
                    String message = "Rate limit exceeded for node execution";
                    log.warn("[policyKey={}] {}", policyKey, message);
                    return ExecutionResult.error(ExecutionError.RETRIABLE, message);
                }
                throw cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
            }
        };
    }

    private ExecutionResult executeWithTimeout(TimeLimiter timeLimiter,
                                               Callable<ExecutionResult> callable,
                                               ExecutionContext ctx) throws Exception {
        try {
            return timeLimiter.executeFutureSupplier(() ->
                    CompletableFuture.supplyAsync(() -> {
                        Thread previous = ctx.getExecutionThread();
                        ctx.setExecutionThread(Thread.currentThread());
                        try {
                            return callInner(callable);
                        } finally {
                            ctx.setExecutionThread(previous);
                        }
                    }, asyncExecutor)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
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

    private String resolvePolicyKey(ExecutionContext ctx, NodeDefinition definition) {
        if (ctx != null && ctx.getPluginNodeId() != null) {
            return ctx.getPluginNodeId().toString();
        }
        if (definition != null && definition.getName() != null) {
            return definition.getName();
        }
        return "node";
    }
}
