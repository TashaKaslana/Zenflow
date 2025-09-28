package org.phong.zenflow.plugin.subdomain.resource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot health indicator exposing the state of pooled node resources.
 */
@Component
@RequiredArgsConstructor
public class NodeResourcePoolHealthIndicator implements HealthIndicator {

    private final List<NodeResourcePool<?, ?>> pools;

    @Override
    public Health health() {
        boolean allHealthy = true;
        Map<String, Object> details = new HashMap<>();
        int totalCount = 0;

        for (NodeResourcePool<?, ?> pool : pools) {
            if (pool instanceof BaseNodeResourceManager<?, ?> base) {
                Map<String, Integer> usage = base.getUsageSnapshot();
                Map<String, Object> poolDetails = new HashMap<>();

                for (Map.Entry<String, Integer> entry : usage.entrySet()) {
                    boolean healthy = pool.isResourceHealthy(entry.getKey());
                    if (!healthy) {
                        allHealthy = false;
                    }
                    Map<String, Object> resInfo = new HashMap<>();
                    resInfo.put("activeNodes", entry.getValue());
                    resInfo.put("healthy", healthy);
                    poolDetails.put(entry.getKey(), resInfo);
                    totalCount++;
                }

                if (!poolDetails.isEmpty()) {
                    details.put(pool.getClass().getSimpleName(), poolDetails);
                }
            }
        }

        Health.Builder builder = allHealthy ? Health.up() : Health.down();
        builder.withDetail("totalResourceCount", totalCount);
        builder.withDetails(details);
        return builder.build();
    }
}
