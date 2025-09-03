package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.quartz;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.resource.PollingResponseCache;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.resource.PollingResponseCacheManager;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;

import java.time.Instant;
import java.util.*;

/**
 * Quartz job for polling HTTP endpoints and detecting changes.
 * Uses the generic resource management pattern for response caching.
 */
@Slf4j
@Component
@AllArgsConstructor
public class PollingTriggerJob implements Job {
    private WebClient webClient;
    private PollingResponseCacheManager cacheManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        try {
            UUID triggerId = UUID.fromString(dataMap.getString("triggerId"));
            UUID workflowId = UUID.fromString(dataMap.getString("workflowId"));
            UUID triggerExecutorId = UUID.fromString(dataMap.getString("triggerExecutorId"));
            String url = dataMap.getString("url");
            String httpMethod = dataMap.getString("httpMethod");
            String changeDetectionStrategy = dataMap.getString("changeDetectionStrategy");
            String jsonPath = dataMap.getString("jsonPath");
            Integer timeoutSeconds = dataMap.getIntValue("timeoutSeconds");
            Boolean includeResponse = dataMap.getBooleanValue("includeResponse");

            // Get headers and request body from job data
            @SuppressWarnings("unchecked")
            Map<String, Object> headers = (Map<String, Object>) dataMap.get("headers");
            Object requestBody = dataMap.get("requestBody");

            // Get trigger context from job data
            TriggerContext triggerContext = (TriggerContext) dataMap.get("triggerContext");

            log.debug("Polling endpoint: {} for trigger: {}", url, triggerId);

            // Make HTTP request
            Object response = makeHttpRequest(url, httpMethod, headers, requestBody, timeoutSeconds);

            // Extract data for comparison if jsonPath is specified
            Object dataToCompare = response;
            if (jsonPath != null && !jsonPath.trim().isEmpty()) {
                dataToCompare = extractJsonPath(response, jsonPath);
            }

            // Get response cache using resource manager
            String cacheKey = triggerId.toString();
            Map<String, Object> cacheConfig = new HashMap<>();
            cacheConfig.put("triggerId", triggerId.toString());
            DefaultTriggerResourceConfig resourceConfig = new DefaultTriggerResourceConfig(cacheConfig, "triggerId");

            PollingResponseCache responseCache = cacheManager.getOrCreateResource(cacheKey, resourceConfig);

            // Check for changes
            Object previousData = responseCache.get("lastResponse");
            boolean hasChanged = detectChange(dataToCompare, previousData, changeDetectionStrategy);

            if (hasChanged) {
                log.info("Change detected for polling trigger: {}", triggerId);

                // Update cache with new data
                responseCache.put("lastResponse", dataToCompare);

                // Create payload for workflow
                Map<String, Object> payload = createPayload(url, httpMethod, changeDetectionStrategy,
                                                          jsonPath, response, previousData,
                                                          dataToCompare, includeResponse);

                // Trigger workflow
                triggerContext.startWorkflow(workflowId, triggerExecutorId, payload);
                triggerContext.markTriggered(triggerId, Instant.now());

                log.debug("Workflow triggered for polling change: {}", triggerId);
            } else {
                log.debug("No changes detected for polling trigger: {}", triggerId);
            }

        } catch (WebClientResponseException e) {
            log.error("HTTP error while polling endpoint: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error executing polling job: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }

    /**
     * Makes HTTP request using WebClient
     */
    private Object makeHttpRequest(String url, String httpMethod, Map<String, Object> headers,
                                  Object requestBody, Integer timeoutSeconds) {

        WebClient.RequestBodySpec request = webClient
            .method(HttpMethod.valueOf(httpMethod.toUpperCase()))
            .uri(url);

        // Add headers if provided
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (value != null) {
                    request.header(key, value.toString());
                }
            });
        }

        // Add body for POST/PUT requests
        WebClient.RequestHeadersSpec<?> finalRequest = request;
        if (requestBody != null && (HttpMethod.POST.name().equals(httpMethod.toUpperCase()) ||
                                   HttpMethod.PUT.name().equals(httpMethod.toUpperCase()))) {
            finalRequest = request.bodyValue(requestBody);
        }

        // Execute request with timeout
        return finalRequest
            .retrieve()
            .bodyToMono(Object.class)
            .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
            .block();
    }

    /**
     * Detects if the data has changed based on the strategy
     */
    private boolean detectChange(Object currentData, Object previousData, String strategy) {
        if (previousData == null) {
            return currentData != null;
        }

        return switch (strategy.toLowerCase()) {
            case "hash_comparison" -> !Objects.equals(
                currentData != null ? currentData.hashCode() : null,
                previousData.hashCode()
            );
            case "size_change" -> {
                if (currentData instanceof Collection && previousData instanceof Collection) {
                    yield ((Collection<?>) currentData).size() != ((Collection<?>) previousData).size();
                } else if (currentData instanceof String && previousData instanceof String) {
                    yield ((String) currentData).length() != ((String) previousData).length();
                }
                yield !Objects.equals(currentData, previousData);
            }
            default -> !Objects.equals(currentData, previousData);
        };
    }

    /**
     * Creates payload for workflow trigger
     */
    private Map<String, Object> createPayload(String url, String httpMethod, String changeDetectionStrategy,
                                            String jsonPath, Object response, Object previousData,
                                            Object dataToCompare, Boolean includeResponse) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("polling_url", url);
        payload.put("change_type", determineChangeType(dataToCompare, previousData));
        payload.put("polling_method", httpMethod);
        payload.put("detection_strategy", changeDetectionStrategy);

        if (includeResponse) {
            payload.put("response_data", response);
            payload.put("previous_data", previousData);
        }

        if (jsonPath != null) {
            payload.put("extracted_data", dataToCompare);
            payload.put("json_path", jsonPath);
        }

        return payload;
    }

    /**
     * Determines the type of change that occurred
     */
    private String determineChangeType(Object currentData, Object previousData) {
        if (previousData == null) {
            return "initial_data";
        }

        if (currentData == null) {
            return "data_removed";
        }

        if (currentData instanceof Collection && previousData instanceof Collection) {
            int currentSize = ((Collection<?>) currentData).size();
            int previousSize = ((Collection<?>) previousData).size();

            if (currentSize > previousSize) {
                return "items_added";
            } else if (currentSize < previousSize) {
                return "items_removed";
            } else {
                return "items_modified";
            }
        }

        return "data_changed";
    }

    /**
     * Simple JSON path extraction (basic implementation)
     */
    @SuppressWarnings("unchecked")
    private Object extractJsonPath(Object response, String jsonPath) {
        try {
            if (response instanceof Map && jsonPath.startsWith("$.")) {
                String path = jsonPath.substring(2); // Remove "$."
                Map<String, Object> map = (Map<String, Object>) response;

                String[] parts = path.split("\\.");
                Object current = map;
                for (String part : parts) {
                    if (current instanceof Map) {
                        current = ((Map<String, Object>) current).get(part);
                    } else {
                        return null;
                    }
                }

                return current;
            }
        } catch (Exception e) {
            log.warn("Failed to extract JSON path '{}': {}", jsonPath, e.getMessage());
        }

        return response; // Fallback to full response
    }
}
