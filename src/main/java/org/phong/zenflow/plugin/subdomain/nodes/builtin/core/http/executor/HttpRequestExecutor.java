package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.executor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.exception.HttpExecutorException;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
@Slf4j
public class HttpRequestExecutor implements NodeExecutor {
    private static final Pattern VALID_HEADER_NAME = Pattern.compile("^[!#$%&'*+.^_`|~0-9a-zA-Z-]+$");
    private final WebClient webClient;

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();

        String url = context.read("url", String.class);
        String methodRaw = context.read("method", String.class);
        HttpMethod method = HttpMethod.valueOf(methodRaw != null ? methodRaw : "GET");
        Object body = context.read("body", Object.class);
        if (body == null) {
            body = Map.of();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> rawHeaders = (Map<String, Object>) context.read("headers", Map.class);
        Map<String, Object> headers = ObjectConversion.convertObjectToMap(rawHeaders != null ? rawHeaders : Map.of());

        logs.info("Sending HTTP request to {} with method {}", url, method);

        Map<String, Object> response = webClient.method(method)
                .uri(url)
                .bodyValue(body)
                .headers(httpHeaders -> getHeaders(logs, httpHeaders, headers))
                .exchangeToMono(this::handleResponse)
                .block();

        logs.success("Received response successfully");

        return ExecutionResult.success(response);
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
