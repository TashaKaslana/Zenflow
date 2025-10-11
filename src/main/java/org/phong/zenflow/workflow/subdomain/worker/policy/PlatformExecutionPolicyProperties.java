package org.phong.zenflow.workflow.subdomain.worker.policy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "zenflow.execution.policy")
@Component
public class PlatformExecutionPolicyProperties {

    private TimeoutProperties timeout = new TimeoutProperties();
    private RetryProperties retry = new RetryProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();

    @Getter
    @Setter
    public static class TimeoutProperties {
        /**
         * Default timeout applied when neither node nor workflow overrides it.
         */
        private Duration defaultDuration = Duration.ofMinutes(30);

        /**
         * Maximum timeout allowed for any node execution.
         */
        private Duration maxDuration = Duration.ofHours(1);
    }

    @Getter
    @Setter
    public static class RetryProperties {
        /**
         * Default attempt count (1 = no retry).
         */
        private int defaultMaxAttempts = 1;

        /**
         * Maximum attempt count allowed by the platform.
         */
        private int maxAttempts = 5;

        /**
         * Default wait duration between retries.
         */
        private Duration defaultWaitDuration = Duration.ofSeconds(0);

        /**
         * Maximum wait duration allowed between retries.
         */
        private Duration maxWaitDuration = Duration.ofMinutes(5);
    }

    @Getter
    @Setter
    public static class RateLimitProperties {
        /**
         * Default limit for period (0 = disabled).
         */
        private int defaultLimitForPeriod = 0;

        /**
         * Maximum limit allowed for any node.
         */
        private int maxLimitForPeriod = 1000;

        /**
         * Default refresh period when rate limiting is enabled.
         */
        private Duration defaultRefreshPeriod = Duration.ofSeconds(60);

        /**
         * Default timeout duration when waiting for permits.
         */
        private Duration defaultTimeoutDuration = Duration.ofSeconds(5);
    }
}
