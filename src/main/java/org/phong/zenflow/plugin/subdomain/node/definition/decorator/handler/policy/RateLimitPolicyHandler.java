package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.policy;

import java.util.concurrent.Callable;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.phong.zenflow.workflow.subdomain.worker.policy.ResolvedExecutionPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;

@Component
@Order(0)
public class RateLimitPolicyHandler implements ResiliencePolicyHandler {

    @Override
    public Callable<ExecutionResult> apply(Callable<ExecutionResult> callable,
                                           ResolvedExecutionPolicy policy,
                                           String policyKey,
                                           ExecutionTaskEnvelope envelope) {
        if (!policy.hasRateLimit()) {
            return callable;
        }

        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(policy.getRateLimit().getLimitForPeriod())
                .limitRefreshPeriod(policy.getRateLimit().getRefreshPeriodOrDefault())
                .timeoutDuration(policy.getRateLimit().getTimeoutDurationOrDefault())
                .build();
        RateLimiter rateLimiter = RateLimiter.of(policyKey + "-rate", rateLimiterConfig);
        return RateLimiter.decorateCallable(rateLimiter, callable);
    }
}
