package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class CircuitBreaker {

    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, failing fast
        HALF_OPEN  // Testing if service has recovered
    }

    private final int failureThreshold;
    private final long recoveryTimeMs;
    private final LoggingMetrics metrics;

    @Getter
    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    public CircuitBreaker(LoggingProperties.PersistenceConfig.CircuitBreakerConfig config,
                         LoggingMetrics metrics) {
        this.failureThreshold = config.getFailureThreshold();
        this.recoveryTimeMs = config.getRecoveryTimeMs();
        this.metrics = metrics;
    }

    public <T> T execute(Supplier<T> operation) throws CircuitBreakerException {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > recoveryTimeMs) {
                state = State.HALF_OPEN;
            } else {
                throw new CircuitBreakerException("Circuit breaker is OPEN");
            }
        }

        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    public void executeVoid(Runnable operation) throws CircuitBreakerException {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    private void onSuccess() {
        failureCount.set(0);
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
        }
    }

    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        if (failures >= failureThreshold) {
            state = State.OPEN;
            metrics.incrementCircuitBreakerTrips();
        }
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public static class CircuitBreakerException extends RuntimeException {
        public CircuitBreakerException(String message) {
            super(message);
        }
    }
}
