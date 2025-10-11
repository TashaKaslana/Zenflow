package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@Component
@Order(1)
public class RetryPolicyHandler implements ResiliencePolicyHandler {

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

        retryBuilder.retryOnResult(result -> result != null && result.getStatus() == ExecutionStatus.RETRY);

        Retry retry = Retry.of(policyKey + "-retry", retryBuilder.build());
        return Retry.decorateCallable(retry, callable);
    }
}
