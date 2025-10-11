package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.decorator.ExecutorDecorator;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
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

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

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
                                              ExecutionTaskEnvelope envelope) {
        ResolvedExecutionPolicy policy = policyResolver.resolve(def, envelope.getConfig());
        if (policy.isEmpty()) {
            return inner;
        }

        Callable<ExecutionResult> wrapped = inner;
        String policyKey = resolvePolicyKey(envelope, def);

        wrapped = resolveHierarchyPolicy(envelope, policy, policyKey, wrapped);

        Callable<ExecutionResult> finalWrapped = wrapped;
        return getAppliedPolicyWrapper(finalWrapped, policy, policyKey);
    }

    private Callable<ExecutionResult> getAppliedPolicyWrapper(Callable<ExecutionResult> finalWrapped, ResolvedExecutionPolicy policy, String policyKey) {
        return () -> {
            try {
                return finalWrapped.call();
            } catch (TimeoutException e) {
                String message = "Execution timed out after " + policy.getTimeout();
                logPolicyWarning(policyKey, message);
                return ExecutionResult.error(ExecutionError.TIMEOUT, message);
            } catch (RequestNotPermitted e) {
                String message = "Rate limit exceeded for node execution";
                logPolicyWarning(policyKey, message);
                return ExecutionResult.error(ExecutionError.RETRIABLE, message);
            } catch (MaxRetriesExceededException e) {
                String message = "Maximum retry attempts exhausted";
                logPolicyWarning(policyKey, message, e);
                return ExecutionResult.error(ExecutionError.RETRIABLE, message);
            } catch (CompletionException e) {
                Throwable cause = Optional.ofNullable(e.getCause()).orElse(e);
                if (cause instanceof TimeoutException) {
                    String message = "Execution timed out after " + policy.getTimeout();
                    logPolicyWarning(policyKey, message);
                    return ExecutionResult.error(ExecutionError.TIMEOUT, message);
                }
                if (cause instanceof RequestNotPermitted) {
                    String message = "Rate limit exceeded for node execution";
                    logPolicyWarning(policyKey, message);
                    return ExecutionResult.error(ExecutionError.RETRIABLE, message);
                }
                throw cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
            }
        };
    }

    private Callable<ExecutionResult> resolveHierarchyPolicy(ExecutionTaskEnvelope envelope, ResolvedExecutionPolicy policy, String policyKey, Callable<ExecutionResult> wrapped) {
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
            wrapped = () -> executeWithTimeout(timeLimiter, delegate, envelope);
        }
        return wrapped;
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

    private String resolvePolicyKey(ExecutionTaskEnvelope envelope, NodeDefinition definition) {
        if (envelope.getPluginNodeId() != null) {
            return envelope.getPluginNodeId().toString();
        }
        if (definition != null && definition.getName() != null) {
            return definition.getName();
        }
        return "node";
    }

    private void logPolicyWarning(String policyKey, String message) {
        log.warn("[policyKey={}] {}", policyKey, message);
    }

    private void logPolicyWarning(String policyKey, String message, Throwable throwable) {
        log.warn("[policyKey={}] {}", policyKey, message, throwable);
    }
}
