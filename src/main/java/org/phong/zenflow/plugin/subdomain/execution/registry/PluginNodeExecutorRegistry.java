package org.phong.zenflow.plugin.subdomain.execution.registry;

import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PluginNodeExecutorRegistry {
    private final Map<String, PluginNodeExecutor> executors = new HashMap<>();

    public void register(PluginNodeExecutor executor) {
        executors.put(executor.key(), executor);
    }

    public Optional<PluginNodeExecutor> getExecutor(String key) {
        return Optional.ofNullable(executors.get(key));
    }
}
