package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class RetryPolicyHandler implements ResiliencePolicyHandler {

    private final RuntimeContextManager contextManager;

    @Override
    public Callable<ExecutionResult> apply(Callable<ExecutionResult> callable,
                                           ResolvedExecutionPolicy policy,
                                           String policyKey,
                                           ExecutionTaskEnvelope envelope) {
        if (!policy.hasRetry()) {
            return callable;
        }

        RetryConfig.Builder<ExecutionResult> retryBuilder = RetryConfig.<ExecutionResult>custom()
                .maxAttempts(policy.getRetry().getMaxAttempts())
                .retryExceptions(Exception.class);

        Duration waitDuration = policy.getRetry().getWaitDuration();
        if (waitDuration != null && !waitDuration.isNegative()) {
            retryBuilder.waitDuration(waitDuration);
        }

        // Intercept failures to clear pending writes BEFORE retry
        retryBuilder.retryOnResult(result -> {
            boolean shouldRetry = result != null && result.getStatus() == ExecutionStatus.RETRY;
            
            if (shouldRetry) {
                // Clear pending writes before retry to release RefValue resources
                log.debug("Clearing pending writes before RETRY status for policy: {}", policyKey);
                UUID workflowRunId = envelope.getContext().getWorkflowRunId();
                RuntimeContext runtimeContext = contextManager.getOrCreate(workflowRunId.toString());
                runtimeContext.clearPendingWrites();
            }
            
            return shouldRetry;
        });
        
        // Wrap callable to clear pending writes on exception before retry
        Callable<ExecutionResult> wrappedCallable = () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                // Clear pending writes before exception-triggered retry
                log.debug("Clearing pending writes before exception retry for policy: {}", policyKey);
                UUID workflowRunId = envelope.getContext().getWorkflowRunId();
                RuntimeContext runtimeContext = contextManager.getOrCreate(workflowRunId.toString());
                runtimeContext.clearPendingWrites();
                throw e;
            }
        };

        Retry retry = Retry.of(policyKey + "-retry", retryBuilder.build());
        return Retry.decorateCallable(retry, wrappedCallable);
    }
}
