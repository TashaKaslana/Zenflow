package org.phong.zenflow.plugin.subdomain.nodes.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.executor.HttpRequestExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class RemoteNodeExecutor implements NodeExecutor {
    private final HttpRequestExecutor httpRequestExecutor;

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        Map<String, Object> entrypoint = context.getCurrentNodeEntrypoint();

        String url = context.read("url", String.class);
        String method = context.read("method", String.class);
        if (method == null) {
            method = "POST";
        }

        if (url == null || url.isBlank()) {
            throw new ExecutorException("Remote node is missing entrypoint.url");
        }

        Map<String, String> headers = new HashMap<>();
        Object rawHeadersObj = context.read("headers", Object.class);
        if (rawHeadersObj != null) {
            Map<String, Object> rawHeaders = ObjectConversion.convertObjectToMap(rawHeadersObj);
            rawHeaders.forEach((k, v) -> headers.put(k, String.valueOf(v)));
        }

        Object secretsObj = context.read("secrets", Object.class);
        if (secretsObj != null) {
            List<Map<String, Object>> secrets = ObjectConversion.safeConvert(secretsObj, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> secret : secrets) {
                String key = (String) secret.get("key");
                String injectAs = (String) secret.getOrDefault("inject_as", "header");
                Boolean required = (Boolean) secret.getOrDefault("required", true);

                String rawKey = "secret." + key;
                Object rawValue = context.read(rawKey, Object.class); // Read secret from context
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
        // The HttpRequestExecutor now reads directly from the context.
        // So, we need to write the parameters to the context before executing HttpRequestExecutor.
        context.write("url", url);
        context.write("method", method);
        context.write("headers", headers);
        // The original code passed the whole `input` map as body.
        // `input` was `config.input()`.
        // `config.input()` contains all the parameters of the current node.
        // So, I need to pass the `entrypoint` map as the body.
        context.write("body", entrypoint);


        ExecutionResult httpResult = httpRequestExecutor.execute(context);

        if (httpResult.getStatus() != ExecutionStatus.SUCCESS) {
            return httpResult;
        }

        // HttpRequestExecutor now writes to context, so read from there
        Object remoteResponseBody = context.read("body", Object.class);

        try {
            if (remoteResponseBody instanceof Map && ((Map<?, ?>) remoteResponseBody).containsKey("status")) {
                return ObjectConversion.safeConvert(remoteResponseBody, ExecutionResult.class);
            } else {
                context.write("response", remoteResponseBody);
                return ExecutionResult.success();
            }
        } catch (Exception e) {
            context.write("response", remoteResponseBody);
            return ExecutionResult.success();
        }
    }
}
