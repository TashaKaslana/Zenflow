package org.phong.zenflow.plugin.subdomain.execution.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class PluginNodeExecutorRegistry {

    private final Cache<PluginNodeIdentifier, PluginNodeExecutor> executorCache =
            Caffeine.newBuilder().build();

    private final Map<PluginNodeIdentifier, Supplier<PluginNodeExecutor>> executorSuppliers =
            new ConcurrentHashMap<>();

    public void register(PluginNodeIdentifier identifier, Supplier<PluginNodeExecutor> supplier) {
        executorSuppliers.put(identifier, supplier);
    }

    public Optional<PluginNodeExecutor> getExecutor(PluginNodeIdentifier identifier) {
        return Optional.ofNullable(executorCache.get(identifier, id -> {
            Supplier<PluginNodeExecutor> supplier = executorSuppliers.get(id);
            return supplier != null ? supplier.get() : null;
        }));
    }
}
