package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.vertexai.VertexAI;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.core.GcpCredentialsProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
@AllArgsConstructor
public class GeminiVertexResourceManager extends BaseNodeResourceManager<VertexAiGeminiChatModel, GeminiVertexResourceManager.GeminiVertexConfig> {

    private final AiToolRegistry toolRegistry;
    private final AiObservationRegistry observationRegistry;
    private final ObjectMapper objectMapper;
    private static final List<String> CLOUD_PLATFORM_SCOPES = List.of(
            "https://www.googleapis.com/auth/cloud-platform"
    );

    @Override
    public GeminiVertexConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        // Read from profile - simple API key pattern + OAuth secrets
        String apiKey = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.API_KEY);
        String projectId = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.PROJECT_ID);
        String region = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.REGION);
        String serviceAccountJson = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.SERVICE_ACCOUNT_JSON);
        String oauthClientId = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.CLIENT_ID);
        String oauthClientSecret = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.CLIENT_SECRET);
        String oauthRefreshToken = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.REFRESH_TOKEN);
        String baseUrl = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.BASE_URL);
        
        boolean hasApiKey = StringUtils.hasText(apiKey);
        boolean hasServiceAccount = StringUtils.hasText(serviceAccountJson);
        boolean hasRefreshCredentials = StringUtils.hasText(oauthClientId)
                && StringUtils.hasText(oauthClientSecret)
                && StringUtils.hasText(oauthRefreshToken);

        if (!hasApiKey && !hasServiceAccount && !hasRefreshCredentials) {
            throw new IllegalStateException("No credentials found in AI profile. Provide API_KEY, SERVICE_ACCOUNT_JSON, or OAuth refresh-token secrets.");
        }
        
        // For Gemini, we need project ID (can be extracted from API key or provided separately)
        JsonNode serviceAccountNode = parseServiceAccountJsonNode(serviceAccountJson);
        if (!StringUtils.hasText(projectId)) {
            projectId = extractProjectIdFromServiceAccount(serviceAccountNode);
        }

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("PROJECT_ID not found in profile. Gemini requires a GCP project ID.");
        }
        
        // Region defaults to us-central1 if not provided
        if (region == null || region.isBlank()) {
            region = "us-central1";
        }
        
        // Read model from node config (optional, defaults to gemini-2.0-flash)
        String model = ctx.readOrDefault("model", String.class, "gemini-2.0-flash");
        
        return new GeminiVertexConfig(apiKey, projectId, region, model,
                serviceAccountJson,
                oauthClientId,
                oauthClientSecret,
                oauthRefreshToken,
                baseUrl);
    }

    @Override
    protected VertexAiGeminiChatModel createResource(String resourceKey, GeminiVertexConfig config) {
        log.info("Creating new Gemini chat model for key: {}", resourceKey);
        
        // Build credentials from profile secrets
        GoogleCredentials credentials = buildGoogleCredentials(config);
        
        // Build VertexAI client with credentials
        VertexAI vertexAI = buildVertexClient(config, credentials);
        
        // Build chat options
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model(config.model())
                .temperature(0.7)
                .build();

        // Build tool calling manager using the toolkit of registered tools
        ToolCallingManager toolCallingManager = buildToolCallingManager(toolRegistry.copy());

        // Create and return chat model with Spring AI parameters
        return VertexAiGeminiChatModel.builder()
                .vertexAI(vertexAI)
                .defaultOptions(options)
                .toolCallingManager(toolCallingManager)
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .observationRegistry(observationRegistry.getRegistry())
                .build();
    }

    @Override
    protected void cleanupResource(VertexAiGeminiChatModel resource) {
        log.info("Cleaning up Gemini chat model resource");
        // The VertexAI client will be closed automatically
        // Spring AI manages the lifecycle internally
    }

    @Override
    protected boolean checkResourceHealth(VertexAiGeminiChatModel resource) {
        // Basic health check - just verify the resource is not null
        return resource != null;
    }

    private VertexAI buildVertexClient(GeminiVertexConfig config, GoogleCredentials credentials) {
        VertexAI.Builder builder = new VertexAI.Builder()
                .setProjectId(config.projectId())
                .setLocation(config.region())
                .setCredentials(credentials);

        if (StringUtils.hasText(config.baseUrl())) {
            builder.setApiEndpoint(config.baseUrl());
        }

        return builder.build();
    }

    private GoogleCredentials buildGoogleCredentials(GeminiVertexConfig config) {
        try {
            JsonNode parsedJson = parseServiceAccountJsonNode(config.serviceAccountJson());
            if (parsedJson != null) {
                if (!"service_account".equals(parsedJson.path("type").asText(null))) {
                    throw new IllegalStateException("SERVICE_ACCOUNT_JSON must describe a service account (type=\"service_account\").");
                }
                try (ByteArrayInputStream stream = new ByteArrayInputStream(config.serviceAccountJson()
                        .getBytes(StandardCharsets.UTF_8))) {
                    return GoogleCredentials.fromStream(stream).createScoped(CLOUD_PLATFORM_SCOPES);
                }
            }

            if (StringUtils.hasText(config.oauthClientId()) &&
                    StringUtils.hasText(config.oauthClientSecret()) &&
                    StringUtils.hasText(config.oauthRefreshToken())) {
                return UserCredentials.newBuilder()
                        .setClientId(config.oauthClientId())
                        .setClientSecret(config.oauthClientSecret())
                        .setRefreshToken(config.oauthRefreshToken())
                        .build()
                        .createScoped(CLOUD_PLATFORM_SCOPES);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build Google credentials for Gemini", e);
        }

        if (StringUtils.hasText(config.apiKey())) {
            throw new IllegalStateException("Gemini requires OAuth2 credentials; API keys are not supported. Provide SERVICE_ACCOUNT_JSON or OAuth refresh-token credentials.");
        }

        throw new IllegalStateException("No valid Google credentials configured for Gemini; please supply SERVICE_ACCOUNT_JSON or OAuth refresh-token secrets.");
    }

    private JsonNode parseServiceAccountJsonNode(String serviceAccountJson) {
        if (!StringUtils.hasText(serviceAccountJson)) {
            return null;
        }

        try {
            return objectMapper.readTree(serviceAccountJson);
        } catch (IOException e) {
            log.warn("Unable to parse SERVICE_ACCOUNT_JSON while extracting project_id", e);
            return null;
        }
    }

    private String extractProjectIdFromServiceAccount(JsonNode root) {
        if (root == null) {
            return null;
        }

        JsonNode candidate = root;
        if (candidate.has("web")) {
            candidate = candidate.get("web");
        } else if (candidate.has("installed")) {
            candidate = candidate.get("installed");
        }

        JsonNode projectNode = candidate.get("project_id");
        if (projectNode != null && projectNode.isTextual()) {
            String text = projectNode.asText();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }

        return null;
    }

    private ToolCallingManager buildToolCallingManager(AiToolRegistry registry) {
        ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(registry.getToolObjects())
                .build()
                .getToolCallbacks();

        return ToolCallingManager.builder()
                .toolCallbackResolver(new StaticToolCallbackResolver(Arrays.asList(toolCallbacks)))
                .observationRegistry(observationRegistry.getRegistry())
                .build();
    }

    /**
         * Configuration for Gemini resources.
         * Simple API key pattern - works like OpenAI SDK.
         */
    public record GeminiVertexConfig(String apiKey, String projectId, String region, String model,
                                     String serviceAccountJson, String oauthClientId, String oauthClientSecret,
                                     String oauthRefreshToken, String baseUrl) implements ResourceConfig {
        @Override
        public String getResourceIdentifier() {
            // Pool by project + region + model
            return projectId + ":" + region + ":" + model;
        }

        @Override
        public Map<String, Object> getContextMap() {
            return Map.of(
                    "project_id", projectId,
                    "region", region,
                    "model", model,
                    "credential_source", getCredentialSource()
            );
        }

        public String getCredentialSource() {
            if (StringUtils.hasText(serviceAccountJson)) {
                return "service_account";
            }
            if (StringUtils.hasText(oauthClientId) &&
                    StringUtils.hasText(oauthClientSecret) &&
                    StringUtils.hasText(oauthRefreshToken)) {
                return "oauth_refresh_token";
            }
            if (StringUtils.hasText(apiKey)) {
                return "api_key";
            }
            return "unknown";
        }
    }
}
