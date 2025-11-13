package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.core.GeminiCredentialsProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class GeminiResourceManager extends BaseNodeResourceManager<GeminiChatModel, GeminiResourceManager.GeminiApiConfig> {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    @Override
    public GeminiApiConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        String apiKey = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.API_KEY);
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Gemini API requires an API_KEY secret in the ai-credentials profile.");
        }

        String baseUrl = ctx.readOrDefault("api_host", String.class, null);
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.BASE_URL);
        }
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = DEFAULT_BASE_URL;
        }

        String model = ctx.readOrDefault("model", String.class, "gemini-2.0-flash");

        return new GeminiApiConfig(apiKey, normalizeBaseUrl(baseUrl), model);
    }

    @Override
    protected GeminiChatModel createResource(String resourceKey, GeminiApiConfig config) {
        log.info("Creating Gemini API chat model for key: {}", resourceKey);

        return new GeminiChatModel(config.apiKey, config.baseUrl, config.model);
    }

    @Override
    protected void cleanupResource(GeminiChatModel resource) {
        log.info("Cleaning up Gemini API chat model resource");
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return trimmed;
    }

    public record GeminiApiConfig(String apiKey, String baseUrl, String model) implements ResourceConfig {

        @Override
        public String getResourceIdentifier() {
            return baseUrl + model + apiKey.hashCode();
        }

        @Override
        public Map<String, Object> getContextMap() {
            return Map.of(
                    "base_url", baseUrl,
                    "model", model
            );
        }
    }
}
