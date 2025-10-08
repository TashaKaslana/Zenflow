package org.phong.zenflow.plugin.subdomain.execution.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class PluginNodeExecutorRegistry {

    private final Cache<@NonNull String, NodeDefinition> definitionCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.of(30, ChronoUnit.MINUTES))
            .maximumSize(1000)
            .build();

    private final Map<String, Supplier<NodeDefinition>> definitionSuppliers =
            new ConcurrentHashMap<>();

    public void register(String identifier, Supplier<NodeDefinition> supplier) {
        definitionSuppliers.put(identifier, supplier);
    }

    public Optional<NodeDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitionCache.get(id, cacheKey -> {
            Supplier<NodeDefinition> supplier = definitionSuppliers.get(cacheKey);
            return supplier != null ? supplier.get() : null;
        }));
    }
}
