package org.phong.zenflow.plugin.subdomain.executors.builtin.http.executor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.http.exception.HttpExecutorException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
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
        return "core.http_request";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        List<String> logs = new ArrayList<>();
        try {
            String url = (String) config.get("url");
            HttpMethod method = HttpMethod.valueOf((String) config.get("method"));
            String body = (String) config.getOrDefault("body", "");
            Map<String, Object> headers = ObjectConversion.convertObjectToMap(config.getOrDefault("headers", Map.of()));

            logs.add("Sending HTTP request to " + url + " with method " + method);

            Object response = webClient.method(method)
                    .uri(url)
                    .bodyValue(body)
                    .headers(httpHeaders -> getHeaders(httpHeaders, headers))
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            logs.add("Received response successfully");

            return ExecutionResult.success(Map.of("response", response != null ? response : "No response"), logs);
        } catch (WebClientResponseException e) {
            logs.add("HTTP error with status " + e.getStatusCode());
            log.debug("HTTP error with status {}", e.getStatusCode());
            return ExecutionResult.error(e.getResponseBodyAsString(), logs);
        } catch (Exception e) {
            logs.add("Unexpected error occurred");
            log.debug("Unexpected error during HTTP request execution", e);
            return ExecutionResult.error(e.getMessage(), logs);
        }
    }

    private void getHeaders(HttpHeaders httpHeaders, Map<String, Object> headers) {
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!VALID_HEADER_NAME.matcher(key).matches()) {
                    throw new HttpExecutorException("Invalid HTTP header name: " + key);
                }

                if (value instanceof String) {
                    httpHeaders.set(key, (String) value);
                } else {
                    throw new HttpExecutorException("Unsupported header value type: " + value.getClass());
                }
            });
        }
    }
}