package org.phong.zenflow.plugin.subdomain.resource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micrometer metrics exposing usage statistics for {@link NodeResourcePool} implementations.
 */
@Component
@RequiredArgsConstructor
public class NodeResourcePoolMetrics implements MeterBinder {

    private final List<NodeResourcePool<?, ?>> pools;
    private final Map<NodeResourcePool<?, ?>, MultiGauge> gauges = new ConcurrentHashMap<>();

    @Override
    public void bindTo(@NotNull MeterRegistry registry) {
        for (NodeResourcePool<?, ?> pool : pools) {
            if (pool instanceof BaseNodeResourceManager<?, ?> base) {
                String poolName = pool.getClass().getSimpleName();
                Gauge.builder("node.resources.total", () -> base.getUsageSnapshot().size())
                        .description("Number of active resources in the pool")
                        .tag("pool", poolName)
                        .register(registry);
                MultiGauge mg = MultiGauge.builder("node.resource.nodes")
                        .description("Active node count for resource key")
                        .tag("pool", poolName)
                        .register(registry);
                gauges.put(pool, mg);
                updateGauge(base, mg);
            }
        }
    }

    @Scheduled(fixedDelayString = "${metrics.nodeResource.refresh:10000}")
    public void refresh() {
        gauges.forEach((pool, mg) -> {
            if (pool instanceof BaseNodeResourceManager<?, ?> base) {
                updateGauge(base, mg);
            }
        });
    }

    private void updateGauge(BaseNodeResourceManager<?, ?> base, MultiGauge mg) {
        List<MultiGauge.Row<?>> rows = new java.util.ArrayList<>();
        base.getUsageSnapshot().forEach((k, v) ->
                rows.add(MultiGauge.Row.of(Tags.of("resource", k), v))
        );
        mg.register(rows, true);
    }
}
