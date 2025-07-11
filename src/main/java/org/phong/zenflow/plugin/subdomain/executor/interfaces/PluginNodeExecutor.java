package org.phong.zenflow.plugin.subdomain.executor.interfaces;

import org.phong.zenflow.plugin.subdomain.executor.dto.ExecutionResult;

import java.util.Map;

public interface PluginNodeExecutor {
    // e.g., "Core:HTTP Request"
    String key();

    // execute the plugin node with the given configuration and context
    ExecutionResult execute(Map<String, Object> config, Map<String, Object> context);
}
