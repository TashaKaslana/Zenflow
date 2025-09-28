package org.phong.zenflow.plugin.subdomain.execution.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class PluginNodeExecutorRegistry {

    private final Cache<@NonNull String, PluginNodeExecutor> executorCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.of(30, ChronoUnit.MINUTES))
            .maximumSize(1000)
            .build();

    private final Map<String, Supplier<PluginNodeExecutor>> executorSuppliers =
            new ConcurrentHashMap<>();

    public void register(String identifier, Supplier<PluginNodeExecutor> supplier) {
        executorSuppliers.put(identifier, supplier);
    }

    public Optional<PluginNodeExecutor> getExecutor(String id) {
        return Optional.ofNullable(executorCache.get(id, cacheKey -> {
            Supplier<PluginNodeExecutor> supplier = executorSuppliers.get(cacheKey);
            return supplier != null ? supplier.get() : null;
        }));
    }
}
