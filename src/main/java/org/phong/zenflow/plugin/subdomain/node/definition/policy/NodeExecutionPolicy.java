package org.phong.zenflow.plugin.subdomain.node.definition.policy;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
@Builder
public class NodeExecutionPolicy {
    private final Duration timeout;
    private final RetryPolicy retry;
    private final RateLimitPolicy rateLimit;

    public boolean hasTimeout() {
        return timeout != null && !timeout.isZero() && !timeout.isNegative();
    }

    public boolean hasRetry() {
        return retry != null && retry.getMaxAttempts() != null && retry.getMaxAttempts() > 1;
    }

    public boolean hasRateLimit() {
        return rateLimit != null && rateLimit.getLimitForPeriod() != null && rateLimit.getLimitForPeriod() > 0;
    }

    @Getter
    @Builder
    public static class RetryPolicy {
        private final Integer maxAttempts;
        private final Duration waitDuration;
    }

    @Getter
    @Builder
    public static class RateLimitPolicy {
        private final Integer limitForPeriod;
        private final Duration refreshPeriod;
        private final Duration timeoutDuration;

        public Duration getRefreshPeriodOrDefault() {
            return refreshPeriod != null ? refreshPeriod : Duration.ofSeconds(1);
        }

        public Duration getTimeoutDurationOrDefault() {
            return timeoutDuration != null ? timeoutDuration : Duration.ofSeconds(1);
        }
    }

    public static NodeExecutionPolicy empty() {
        return NodeExecutionPolicy.builder().build();
    }
}
