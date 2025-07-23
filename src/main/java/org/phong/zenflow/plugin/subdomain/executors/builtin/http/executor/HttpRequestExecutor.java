package org.phong.zenflow.plugin.subdomain.executors.builtin.http.executor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.http.exception.HttpExecutorException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
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

@Component
@AllArgsConstructor
@Slf4j
public class HttpRequestExecutor implements PluginNodeExecutor {
    private static final Pattern VALID_HEADER_NAME = Pattern.compile("^[!#$%&'*+.^_`|~0-9a-zA-Z-]+$");
    private final WebClient webClient;

    @Override
    public String key() {
        return "core:http.request";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config) {
        LogCollector logs = new LogCollector();
        try {
            Map<String, Object> input = ObjectConversion.convertObjectToMap(config.input());

            String url = (String) input.get("url");
            HttpMethod method = HttpMethod.valueOf((String) input.get("method"));
            Object body = input.getOrDefault("body", Map.of());
            Map<String, Object> headers = ObjectConversion.convertObjectToMap(input.getOrDefault("headers", Map.of()));

            logs.info("Sending HTTP request to " + url + " with method " + method);

            Map<String, Object> response = webClient.method(method)
                    .uri(url)
                    .bodyValue(body)
                    .headers(httpHeaders -> getHeaders(logs, httpHeaders, headers))
                    .exchangeToMono(this::handleResponse)
                    .block();

            logs.success("Received response successfully");

            return ExecutionResult.success(response, logs.getLogs());
        } catch (WebClientResponseException e) {
            logs.error("HTTP error with status {}", e.getStatusCode());
            log.debug("HTTP error with status {}", e.getStatusCode());
            return ExecutionResult.error(e.getResponseBodyAsString(), logs.getLogs());
        } catch (Exception e) {
            logs.error("Unexpected error occurred: {}", e.getMessage());
            log.debug("Unexpected error during HTTP request execution", e);
            return ExecutionResult.error(e.getMessage(), logs.getLogs());
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

    private void getHeaders(LogCollector logs, HttpHeaders httpHeaders, Map<String, Object> headers) {
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