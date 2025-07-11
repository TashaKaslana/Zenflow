package org.phong.zenflow.plugin.subdomain.executor.builtin;

import org.phong.zenflow.plugin.subdomain.executor.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.executor.interfaces.PluginNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpRequestExecutor implements PluginNodeExecutor {

    @Override
    public String key() {
        return "Core:HTTP Request";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {

        return new ExecutionResult("ok", Map.of("response", "..."));
    }
}
