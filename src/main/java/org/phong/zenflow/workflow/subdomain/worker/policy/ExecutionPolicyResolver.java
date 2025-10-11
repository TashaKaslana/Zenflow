
package org.phong.zenflow.workflow.subdomain.worker.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.policy.NodeExecutionPolicy;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionPolicyResolver {

    private final PlatformExecutionPolicyManager platformPolicyManager;

    public ResolvedExecutionPolicy resolve(NodeDefinition definition, WorkflowConfig config) {
        PlatformExecutionPolicyProperties platformProperties = platformPolicyManager.properties();
        NodeExecutionPolicy platformPolicy = platformPolicy(platformProperties);
        NodeExecutionPolicy authorPolicy = definition != null ? definition.getExecutionPolicy() : null;
        NodeExecutionPolicy workflowPolicy = workflowPolicy(config);

        Duration timeout = selectTimeout(platformProperties, platformPolicy, authorPolicy, workflowPolicy);
        NodeExecutionPolicy.RetryPolicy retry = selectRetry(platformProperties, platformPolicy, authorPolicy, workflowPolicy);
        NodeExecutionPolicy.RateLimitPolicy rateLimit = selectRateLimit(platformProperties, platformPolicy, authorPolicy, workflowPolicy);

        return ResolvedExecutionPolicy.builder()
                .timeout(timeout)
                .retry(retry)
                .rateLimit(rateLimit)
                .build();
    }

    private Duration selectTimeout(PlatformExecutionPolicyProperties platformProperties,
                                   NodeExecutionPolicy platformPolicy,
                                   NodeExecutionPolicy authorPolicy,
                                   NodeExecutionPolicy workflowPolicy) {
        Duration candidate = firstNonNull(
                normalizedDuration(workflowPolicy != null ? workflowPolicy.getTimeout() : null),
                normalizedDuration(authorPolicy != null ? authorPolicy.getTimeout() : null),
                normalizedDuration(platformPolicy.getTimeout())
        );

        Duration max = platformProperties.getTimeout().getMaxDuration();
        if (candidate != null && max != null && candidate.compareTo(max) > 0) {
            log.debug("Requested timeout {} exceeds platform maximum {}, capping value", candidate, max);
            candidate = max;
        }
        return candidate;
    }

    private NodeExecutionPolicy.RetryPolicy selectRetry(PlatformExecutionPolicyProperties platformProperties,
                                                        NodeExecutionPolicy platformPolicy,
                                                        NodeExecutionPolicy authorPolicy,
                                                        NodeExecutionPolicy workflowPolicy) {
        NodeExecutionPolicy.RetryPolicy platformRetry = platformPolicy.getRetry();
        NodeExecutionPolicy.RetryPolicy authorRetry = authorPolicy != null ? authorPolicy.getRetry() : null;
        NodeExecutionPolicy.RetryPolicy workflowRetry = workflowPolicy != null ? workflowPolicy.getRetry() : null;

        int maxAllowed = Math.max(platformProperties.getRetry().getMaxAttempts(), 1);
        int defaultAttempts = Math.max(platformProperties.getRetry().getDefaultMaxAttempts(), 1);

        Integer attempts = firstNonNull(
                normalizedAttempts(workflowRetry),
                normalizedAttempts(authorRetry),
                defaultAttempts
        );
        attempts = attempts != null ? attempts : defaultAttempts;

        if (attempts > maxAllowed) {
            log.debug("Requested retry attempts {} exceed platform maximum {}, capping value", attempts, maxAllowed);
            attempts = maxAllowed;
        }

        if (attempts <= 1) {
            return null;
        }

        Duration defaultWait = normalizedDuration(platformRetry != null ? platformRetry.getWaitDuration() : null);
        if (defaultWait == null) {
            defaultWait = normalizedDuration(platformProperties.getRetry().getDefaultWaitDuration());
        }

        Duration wait = firstNonNull(
                normalizedDuration(workflowRetry != null ? workflowRetry.getWaitDuration() : null),
                normalizedDuration(authorRetry != null ? authorRetry.getWaitDuration() : null),
                defaultWait
        );

        Duration maxWait = platformProperties.getRetry().getMaxWaitDuration();
        if (wait != null && maxWait != null && wait.compareTo(maxWait) > 0) {
            log.debug("Requested retry wait duration {} exceeds platform maximum {}, capping value", wait, maxWait);
            wait = maxWait;
        }

        return NodeExecutionPolicy.RetryPolicy.builder()
                .maxAttempts(attempts)
                .waitDuration(wait)
                .build();
    }

    private NodeExecutionPolicy.RateLimitPolicy selectRateLimit(PlatformExecutionPolicyProperties platformProperties,
                                                                NodeExecutionPolicy platformPolicy,
                                                                NodeExecutionPolicy authorPolicy,
                                                                NodeExecutionPolicy workflowPolicy) {
        NodeExecutionPolicy.RateLimitPolicy platformRate = platformPolicy.getRateLimit();
        NodeExecutionPolicy.RateLimitPolicy authorRate = authorPolicy != null ? authorPolicy.getRateLimit() : null;
        NodeExecutionPolicy.RateLimitPolicy workflowRate = workflowPolicy != null ? workflowPolicy.getRateLimit() : null;

        Integer defaultLimit = platformRate != null ? platformRate.getLimitForPeriod() : platformProperties.getRateLimit().getDefaultLimitForPeriod();
        Integer limit = firstNonNull(
                normalizedLimit(workflowRate),
                normalizedLimit(authorRate),
                defaultLimit
        );

        if (limit == null || limit <= 0) {
            return null;
        }

        int maxLimit = Math.max(platformProperties.getRateLimit().getMaxLimitForPeriod(), 1);
        if (limit > maxLimit) {
            log.debug("Requested rate limit {} exceeds platform maximum {}, capping value", limit, maxLimit);
            limit = maxLimit;
        }

        Duration defaultRefresh = normalizedDuration(platformRate != null ? platformRate.getRefreshPeriod() : null);
        if (defaultRefresh == null) {
            defaultRefresh = normalizedDuration(platformProperties.getRateLimit().getDefaultRefreshPeriod());
        }

        Duration refresh = firstNonNull(
                normalizedDuration(workflowRate != null ? workflowRate.getRefreshPeriod() : null),
                normalizedDuration(authorRate != null ? authorRate.getRefreshPeriod() : null),
                defaultRefresh
        );

        Duration defaultTimeout = normalizedDuration(platformRate != null ? platformRate.getTimeoutDuration() : null);
        if (defaultTimeout == null) {
            defaultTimeout = normalizedDuration(platformProperties.getRateLimit().getDefaultTimeoutDuration());
        }

        Duration timeout = firstNonNull(
                normalizedDuration(workflowRate != null ? workflowRate.getTimeoutDuration() : null),
                normalizedDuration(authorRate != null ? authorRate.getTimeoutDuration() : null),
                defaultTimeout
        );

        return NodeExecutionPolicy.RateLimitPolicy.builder()
                .limitForPeriod(limit)
                .refreshPeriod(refresh)
                .timeoutDuration(timeout)
                .build();
    }

    private NodeExecutionPolicy platformPolicy(PlatformExecutionPolicyProperties platformProperties) {
        PlatformExecutionPolicyProperties.TimeoutProperties timeoutProps = platformProperties.getTimeout();
        PlatformExecutionPolicyProperties.RetryProperties retryProps = platformProperties.getRetry();
        PlatformExecutionPolicyProperties.RateLimitProperties rateProps = platformProperties.getRateLimit();

        return NodeExecutionPolicy.builder()
                .timeout(normalizedDuration(timeoutProps.getDefaultDuration()))
                .retry(NodeExecutionPolicy.RetryPolicy.builder()
                        .maxAttempts(Math.max(retryProps.getDefaultMaxAttempts(), 1))
                        .waitDuration(normalizedDuration(retryProps.getDefaultWaitDuration()))
                        .build())
                .rateLimit(NodeExecutionPolicy.RateLimitPolicy.builder()
                        .limitForPeriod(rateProps.getDefaultLimitForPeriod())
                        .refreshPeriod(normalizedDuration(rateProps.getDefaultRefreshPeriod()))
                        .timeoutDuration(normalizedDuration(rateProps.getDefaultTimeoutDuration()))
                        .build())
                .build();
    }

    private NodeExecutionPolicy workflowPolicy(WorkflowConfig config) {
        if (config == null || config.input() == null) {
            return NodeExecutionPolicy.empty();
        }

        Map<String, Object> policySection = extractPolicySection(config.input());
        if (policySection.isEmpty()) {
            return NodeExecutionPolicy.empty();
        }

        return NodeExecutionPolicy.builder()
                .timeout(parseDuration(policySection.get("timeout"), policySection))
                .retry(parseRetryPolicy(policySection.get("retry")))
                .rateLimit(parseRateLimitPolicy(policySection.get("rateLimit")))
                .build();
    }

    private Map<String, Object> extractPolicySection(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<?, ?> rawSection = null;
        Object value = input.get("policy");
        if (value instanceof Map<?, ?> policyMap) {
            rawSection = policyMap;
        } else {
            value = input.get("policies");
            if (value instanceof Map<?, ?> policiesMap) {
                rawSection = policiesMap;
            }
        }

        if (rawSection == null || rawSection.isEmpty()) {
            return Collections.emptyMap();
        }

        java.util.Map<String, Object> normalized = new java.util.HashMap<>();
        rawSection.forEach((key, val) -> {
            if (key instanceof String strKey) {
                normalized.put(strKey, val);
            }
        });
        return normalized;
    }

    private NodeExecutionPolicy.RetryPolicy parseRetryPolicy(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Integer maxAttempts = parseInteger(map.get("maxAttempts"));
        Duration wait = parseDuration(map.get("waitDuration"), map);
        if (wait == null) {
            wait = parseDuration(map.get("waitMs"), map);
        }
        return NodeExecutionPolicy.RetryPolicy.builder()
                .maxAttempts(maxAttempts)
                .waitDuration(wait)
                .build();
    }

    private NodeExecutionPolicy.RateLimitPolicy parseRateLimitPolicy(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }

        Integer limit = parseInteger(map.get("limitForPeriod"));
        if (limit == null) {
            limit = parseInteger(map.get("limit"));
        }

        Duration refresh = parseDuration(map.get("refreshPeriod"), map);
        Duration timeout = parseDuration(map.get("timeoutDuration"), map);
        if (timeout == null) {
            timeout = parseDuration(map.get("timeout"), map);
        }

        return NodeExecutionPolicy.RateLimitPolicy.builder()
                .limitForPeriod(limit)
                .refreshPeriod(refresh)
                .timeoutDuration(timeout)
                .build();
    }

    private Integer normalizedAttempts(NodeExecutionPolicy.RetryPolicy retry) {
        if (retry == null || retry.getMaxAttempts() == null) {
            return null;
        }
        return Math.max(retry.getMaxAttempts(), 1);
    }

    private Integer normalizedLimit(NodeExecutionPolicy.RateLimitPolicy rateLimit) {
        if (rateLimit == null || rateLimit.getLimitForPeriod() == null) {
            return null;
        }
        return rateLimit.getLimitForPeriod();
    }

    private Duration normalizedDuration(Duration duration) {
        if (duration == null) {
            return null;
        }
        if (duration.isZero() || duration.isNegative()) {
            return null;
        }
        return duration;
    }

    private Duration parseDuration(Object candidate, Map<?, ?> context) {
        return switch (candidate) {
            case null -> null;
            case Number number -> normalizeDurationMillis(number.longValue());
            case String str -> parseDurationString(str.trim());
            case Map<?, ?> map -> {
                Object value = map.get("value");
                Object unit = map.get("unit");
                yield parseDurationFromValueUnit(value, unit);
            }
            default -> {
                log.debug("Unsupported duration format: {} (context keys: {})", candidate, context != null ? context.keySet() : "unknown");
                yield null;
            }
        };
    }

    private Duration parseDurationFromValueUnit(Object value, Object unit) {
        if (!(value instanceof Number number)) {
            return null;
        }
        long base = number.longValue();
        if (base <= 0) {
            return null;
        }

        if (unit instanceof String str) {
            return switch (str.toLowerCase()) {
                case "ms", "millis", "milliseconds" -> Duration.ofMillis(base);
                case "s", "sec", "second", "seconds" -> Duration.ofSeconds(base);
                case "m", "min", "minute", "minutes" -> Duration.ofMinutes(base);
                case "h", "hour", "hours" -> Duration.ofHours(base);
                default -> null;
            };
        }

        return null;
    }

    private Duration parseDuration(Object candidate) {
        return parseDuration(candidate, null);
    }

    private Duration parseDurationString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        try {
            if (candidate.matches("^-?\\d+$")) {
                return normalizeDurationMillis(Long.parseLong(candidate));
            }
            if (candidate.endsWith("ms")) {
                return normalizeDurationMillis(Long.parseLong(candidate.substring(0, candidate.length() - 2).trim()));
            }
            String trimStrSub1 = candidate.substring(0, candidate.length() - 1).trim();
            if (candidate.endsWith("s")) {
                return normalizeDurationSeconds(trimStrSub1);
            }
            if (candidate.endsWith("m")) {
                return normalizeDurationMinutes(trimStrSub1);
            }
            if (candidate.endsWith("h")) {
                return normalizeDurationHours(trimStrSub1);
            }
            return normalizedDuration(Duration.parse(candidate));
        } catch (NumberFormatException | DateTimeParseException ex) {
            log.debug("Failed to parse duration '{}': {}", candidate, ex.getMessage());
            return null;
        }
    }

    private Duration normalizeDurationMillis(long millis) {
        if (millis <= 0) {
            return null;
        }
        return Duration.ofMillis(millis);
    }

    private Duration normalizeDurationSeconds(String value) {
        long seconds = Long.parseLong(value);
        if (seconds <= 0) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }

    private Duration normalizeDurationMinutes(String value) {
        long minutes = Long.parseLong(value);
        if (minutes <= 0) {
            return null;
        }
        return Duration.ofMinutes(minutes);
    }

    private Duration normalizeDurationHours(String value) {
        long hours = Long.parseLong(value);
        if (hours <= 0) {
            return null;
        }
        return Duration.ofHours(hours);
    }

    private Integer parseInteger(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case Number number -> {
                return number.intValue();
            }
            case String str when !str.isBlank() -> {
                try {
                    return Integer.parseInt(str.trim());
                } catch (NumberFormatException ex) {
                    log.debug("Failed to parse integer '{}': {}", str, ex.getMessage());
                    return null;
                }
            }
            default -> {
            }
        }
        return null;
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                if (value instanceof String str && str.isBlank()) {
                    continue;
                }
                return value;
            }
        }
        return null;
    }
}
