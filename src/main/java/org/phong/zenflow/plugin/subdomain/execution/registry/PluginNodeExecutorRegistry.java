package org.phong.zenflow.plugin.subdomain.execution.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class PluginNodeExecutorRegistry {

    private final Cache<String, PluginNodeExecutor> executorCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.of(30, ChronoUnit.MINUTES))
            .maximumSize(1000)
            .build();

    private final Map<String, Supplier<PluginNodeExecutor>> executorSuppliers =
            new ConcurrentHashMap<>();

    public void register(PluginNodeIdentifier identifier, Supplier<PluginNodeExecutor> supplier) {
        String key = identifier.toCacheKey();
        executorSuppliers.put(key, supplier);
    }

    public Optional<PluginNodeExecutor> getExecutor(PluginNodeIdentifier identifier) {
        String key = identifier.toCacheKey();
        return Optional.ofNullable(executorCache.get(key, cacheKey -> {
            Supplier<PluginNodeExecutor> supplier = executorSuppliers.get(cacheKey);
            return supplier != null ? supplier.get() : null;
        }));
    }
}
