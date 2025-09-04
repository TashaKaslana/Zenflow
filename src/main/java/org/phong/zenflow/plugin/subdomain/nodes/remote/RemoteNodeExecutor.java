package org.phong.zenflow.plugin.subdomain.nodes.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.executor.HttpRequestExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@PluginNode(
        key = "core:remote",
        name = "Remote Node",
        version = "1.0.0"
)
@AllArgsConstructor
public class RemoteNodeExecutor implements PluginNodeExecutor {

    private final HttpRequestExecutor httpRequestExecutor;
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        Map<String, Object> entrypoint = config.entrypoint();
        Map<String, Object> input = config.input();

        assert entrypoint != null;
        String url = (String) entrypoint.get("url");
        String method = (String) entrypoint.getOrDefault("method", "POST");

        if (url == null || url.isBlank()) {
            throw new ExecutorException("Remote node is missing entrypoint.url");
        }

        Map<String, String> headers = new HashMap<>();
        if (entrypoint.containsKey("headers")) {
            Map<String, Object> rawHeaders = ObjectConversion.convertObjectToMap(entrypoint.get("headers"));
            rawHeaders.forEach((k, v) -> headers.put(k, String.valueOf(v)));
        }

        if (input.containsKey("secrets")) {
            List<Map<String, Object>> secrets = ObjectConversion.safeConvert(config.input().get("secrets"), new TypeReference<>() {
            });
            for (Map<String, Object> secret : secrets) {
                String key = (String) secret.get("key");
                String injectAs = (String) secret.getOrDefault("inject_as", "header");
                Boolean required = (Boolean) secret.getOrDefault("required", true);

                String rawKey = "secret." + key;
                Object rawValue = input.get(rawKey);
                String value = rawValue != null ? String.valueOf(rawValue) : null;

                if (required && (value == null || value.isBlank())) {
                    throw new ExecutorException("Missing required secret: " + rawKey);
                }

                if ("header".equalsIgnoreCase(injectAs) && value != null) {
                    headers.put(key, value);
                }
                // TODO: handle "query", "body", or custom inject types
            }
        }

        // 🔗 Build HTTP request config
        WorkflowConfig httpRequestConfig = new WorkflowConfig(
                Map.of(
                        "url", url,
                        "method", method,
                        "headers", headers,
                        "body", input
                ),
                null,
                null
        );

        ExecutionResult httpResult = httpRequestExecutor.execute(httpRequestConfig, context);

        if (httpResult.getStatus() != ExecutionStatus.SUCCESS) {
            return httpResult;
        }

        Map<String, Object> httpOutput = httpResult.getOutput();
        Object remoteResponseBody = httpOutput.get("body");

        try {
            if (remoteResponseBody instanceof Map && ((Map<?, ?>) remoteResponseBody).containsKey("status")) {
                return ObjectConversion.safeConvert(remoteResponseBody, ExecutionResult.class);
            } else {
                return ExecutionResult.success(Map.of("response", remoteResponseBody));
            }
        } catch (Exception e) {
            return ExecutionResult.success(Map.of("response", remoteResponseBody));
        }
    }
}
