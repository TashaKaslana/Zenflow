package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.executor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.exception.HttpExecutorException;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

@Component
@PluginNode(
        key = "core:http.request",
        name = "HTTP Request",
        version = "1.0.0",
        description = "Executes an HTTP request using the specified method and URL, with optional headers and body.",
        tags = {"http", "request", "network"},
        type = "util",
        icon = "ph:globe",
        schemaPath = "../schema.json",
        docPath = "../doc.md"
)
@AllArgsConstructor
@Slf4j
public class HttpRequestExecutor implements PluginNodeExecutor {
    private static final Pattern VALID_HEADER_NAME = Pattern.compile("^[!#$%&'*+.^_`|~0-9a-zA-Z-]+$");
    private final WebClient webClient;
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();

            String url = (String) input.get("url");
            HttpMethod method = HttpMethod.valueOf((String) input.get("method"));
            Object body = input.getOrDefault("body", Map.of());
            Map<String, Object> headers = ObjectConversion.convertObjectToMap(input.getOrDefault("headers", Map.of()));

            logs.info("Sending HTTP request to {} with method {}", url, method);

            Map<String, Object> response = webClient.method(method)
                    .uri(url)
                    .bodyValue(body)
                    .headers(httpHeaders -> getHeaders(logs, httpHeaders, headers))
                    .exchangeToMono(this::handleResponse)
                    .block();

            logs.success("Received response successfully");

            return ExecutionResult.success(response);
        } catch (WebClientResponseException e) {
            logs.withException(e).error("HTTP error with status {}", e.getStatusCode());
            log.debug("HTTP error with status {}", e.getStatusCode());
            return ExecutionResult.error(e.getResponseBodyAsString());
        } catch (Exception e) {
            logs.withException(e).error("Unexpected error occurred: {}", e.getMessage());
            log.debug("Unexpected error during HTTP request execution", e);
            return ExecutionResult.error(e.getMessage());
        }
    }

    private Mono<Map<String, Object>> handleResponse(ClientResponse response) {
        return response.bodyToMono(Object.class)
                .defaultIfEmpty("No response")
                .map(body -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status_code", response.statusCode().value());
                    result.put("headers", response.headers().asHttpHeaders().toSingleValueMap());
                    result.put("body", body);
                    return result;
                });
    }

    private void getHeaders(NodeLogPublisher logs, HttpHeaders httpHeaders, Map<String, Object> headers) {
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!VALID_HEADER_NAME.matcher(key).matches()) {
                    logs.error("Invalid HTTP header name: " + key);
                    throw new HttpExecutorException("Invalid HTTP header name: " + key);
                }

                if (value instanceof String) {
                    httpHeaders.set(key, (String) value);
                } else {
                    logs.error("Unsupported header value type for key: " + key + ", expected String but got " + value.getClass());
                    throw new HttpExecutorException("Unsupported header value type: " + value.getClass());
                }
            });
        }
    }
}