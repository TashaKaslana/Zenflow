package org.phong.zenflow.workflow.subdomain.worker.policy;

import lombok.Builder;
import lombok.Getter;
import org.phong.zenflow.plugin.subdomain.node.definition.policy.NodeExecutionPolicy;

import java.time.Duration;

@Getter
@Builder
public class ResolvedExecutionPolicy {
    private final Duration timeout;
    private final NodeExecutionPolicy.RetryPolicy retry;
    private final NodeExecutionPolicy.RateLimitPolicy rateLimit;

    public boolean hasTimeout() {
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }

    public boolean hasRetry() {
        return retry != null && retry.getMaxAttempts() != null && retry.getMaxAttempts() > 1;
    }

    public boolean hasRateLimit() {
        return rateLimit != null && rateLimit.getLimitForPeriod() != null && rateLimit.getLimitForPeriod() > 0;
    }

    public boolean isEmpty() {
        return !hasTimeout() && !hasRetry() && !hasRateLimit();
    }
}
