package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

//TODO: when spring ai 1.1.0 release, should switch to use the gen-ai which support both api and vertex instead this model
@Slf4j
public class GeminiChatModel {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/";
    private final RestClient restClient;
    private final String model;

    public GeminiChatModel(String apiKey, String model) {
        this(apiKey, DEFAULT_BASE_URL, model);
    }

    public GeminiChatModel(String apiKey, String baseUrl, String model) {
        this.model = model;
        String resolvedBaseUrl = ensureBaseUrl(baseUrl);
        this.restClient = RestClient.builder()
                .baseUrl(resolvedBaseUrl)
                .defaultHeader("X-Goog-Api-Key", apiKey)
                .build();
    }

    /** Send a prompt and parse structured Gemini response. */
    public GeminiResponse call(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri("models/" + model + ":generateContent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return parseResponse(response);

        } catch (RestClientException ex) {
            log.error("Gemini API call failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private GeminiResponse parseResponse(Map<?, ?> map) {
        log.info("Gemini API call response: {}", map);
        if (map == null) return null;
        GeminiResponse r = new GeminiResponse();

        try {
            // Extract text
            var candidates = (List<Map<String, Object>>) map.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                var content = (Map<String, Object>) candidates.getFirst().get("content");
                if (content != null) {
                    var parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        r.text = (String) parts.getFirst().get("text");
                    }
                }
                r.finishReason = (String) candidates.getFirst().get("finishReason");
            }

            // Extract usage metadata
            Map<String, Object> usage = (Map<String, Object>) map.get("usageMetadata");
            if (usage != null) {
                r.promptTokens = toInt(usage.get("promptTokenCount"));
                r.outputTokens = toInt(usage.get("candidatesTokenCount"));
                r.totalTokens = toInt(usage.get("totalTokenCount"));
            }

            // Other metadata
            r.responseId = (String) map.get("responseId");
            r.modelVersion = (String) map.get("modelVersion");

        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage());
        }
        return r;
    }

    /** Simple structured response similar to OpenAI's ChatCompletionResponse */
    @Data
    public static class GeminiResponse {
        private String text;
        private String finishReason;
        private String modelVersion;
        private String responseId;
        private int promptTokens;
        private int outputTokens;
        private int totalTokens;
    }

    private String ensureBaseUrl(String baseUrl) {
        String candidate = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl.trim();
        if (!candidate.endsWith("/")) {
            candidate = candidate + "/";
        }
        return candidate;
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
