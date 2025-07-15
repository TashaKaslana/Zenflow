package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.timeout;

import com.fasterxml.jackson.core.type.TypeReference;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

//TODO: a draft implementation of TimeoutExecutor, consider change status to 'paused' and add time to wakeup instead of sleeping the thread
@Component
public class TimeoutExecutor implements PluginNodeExecutor {

    private static final long MAX_SLEEP_MILLIS = 30_000;

    @Override
    public String key() {
        return "core.timeout";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        try {
            String duration = (String) config.get("duration");
            String unit = (String) config.get("unit");

            if (duration == null || unit == null) {
                throw new IllegalArgumentException("Timeout duration and unit must be specified.");
            }

            long millis = parseDuration(duration, unit);
            Thread.sleep(millis);

            Map<String, Object> nextConfig = ObjectConversion.safeConvert(config.get("next"), new TypeReference<>() {});
            String nextNode = nextConfig != null && !nextConfig.isEmpty() ?
                    (String) nextConfig.get("first") : null;

            return ExecutionResult.nextNode(nextNode);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Timeout interrupted.", e);
        } catch (Exception e) {
            throw new RuntimeException("Invalid timeout configuration: " + e.getMessage(), e);
        }
    }

    private long parseDuration(String duration, String unit) {
        long value = Long.parseLong(duration);

        long millis = switch (unit.toLowerCase()) {
            case "milliseconds" -> value;
            case "seconds" -> value * 1000;
            default -> throw new IllegalArgumentException("Only 'milliseconds' and 'seconds' are allowed.");
        };

        if (millis > MAX_SLEEP_MILLIS) {
            throw new IllegalArgumentException("Timeout duration exceeds limit of " + MAX_SLEEP_MILLIS + "ms.");
        }

        return millis;
    }
}
