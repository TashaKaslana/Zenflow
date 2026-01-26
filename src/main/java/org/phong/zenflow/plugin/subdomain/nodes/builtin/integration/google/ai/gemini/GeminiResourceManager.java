package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.core.GeminiCredentialsProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class GeminiResourceManager extends BaseNodeResourceManager<GoogleGenAiChatModel, GeminiResourceManager.GeminiGenAiConfig> {

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final List<String> CLOUD_PLATFORM_SCOPES = List.of(
            "https://www.googleapis.com/auth/cloud-platform"
    );

    private final AiToolRegistry toolRegistry;
    private final AiObservationRegistry observationRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public GeminiGenAiConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        String apiKey = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.API_KEY);
        String projectId = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.PROJECT_ID);
        String region = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.REGION);
        String serviceAccountJson = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.SERVICE_ACCOUNT_JSON);
        String oauthClientId = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.CLIENT_ID);
        String oauthClientSecret = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.CLIENT_SECRET);
        String oauthRefreshToken = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.REFRESH_TOKEN);
        String baseUrl = ctx.readOrDefault("api_host", String.class, null);
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = (String) ctx.getProfileSecret(GeminiCredentialsProfileDescriptor.BASE_URL);
        }

        boolean hasApiKey = StringUtils.hasText(apiKey);
        boolean hasServiceAccount = StringUtils.hasText(serviceAccountJson);
        boolean hasRefreshCredentials = StringUtils.hasText(oauthClientId)
                && StringUtils.hasText(oauthClientSecret)
                && StringUtils.hasText(oauthRefreshToken);

        if (!hasApiKey && !hasServiceAccount && !hasRefreshCredentials) {
            throw new IllegalStateException("No credentials found in AI profile. Provide API_KEY, SERVICE_ACCOUNT_JSON, or OAuth refresh-token secrets.");
        }

        JsonNode serviceAccountNode = parseServiceAccountJsonNode(serviceAccountJson);
        if (!StringUtils.hasText(projectId)) {
            projectId = extractProjectIdFromServiceAccount(serviceAccountNode);
        }

        if (!StringUtils.hasText(region)) {
            region = "us-central1";
        }

        String model = ctx.readOrDefault("model", String.class, "gemini-2.0-flash");

        boolean useVertexAi = hasServiceAccount || hasRefreshCredentials || (!hasApiKey && StringUtils.hasText(projectId));

        if (!useVertexAi && !StringUtils.hasText(baseUrl)) {
            baseUrl = DEFAULT_BASE_URL;
        }

        if (useVertexAi && !StringUtils.hasText(projectId)) {
            throw new IllegalStateException("PROJECT_ID not found in profile. Vertex AI mode requires a GCP project ID.");
        }

        return new GeminiGenAiConfig(apiKey, projectId, region, model, baseUrl,
                serviceAccountJson, oauthClientId, oauthClientSecret, oauthRefreshToken, useVertexAi);
    }

    @Override
    protected GoogleGenAiChatModel createResource(String resourceKey, GeminiGenAiConfig config) {
        log.info("Creating Google GenAI chat model for key: {}", resourceKey);

        Client client = buildClient(config);
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(config.model())
                .build();

        ToolCallingManager toolCallingManager = buildToolCallingManager(toolRegistry.copy());

        return GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(options)
                .toolCallingManager(toolCallingManager)
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .observationRegistry(observationRegistry.getRegistry())
                .build();
    }

    @Override
    protected void cleanupResource(GoogleGenAiChatModel resource) {
        log.info("Cleaning up Gemini chat model resource");
    }

    @Override
    protected boolean checkResourceHealth(GoogleGenAiChatModel resource) {
        return resource != null;
    }

    private Client buildClient(GeminiGenAiConfig config) {
        Client.Builder builder = Client.builder();

        if (config.useVertexAi()) {
            builder.project(config.projectId())
                    .location(config.region())
                    .vertexAI(true);
        } else {
            builder.apiKey(config.apiKey());
        }

        GoogleCredentials credentials = buildGoogleCredentials(config);
        if (credentials != null) {
            builder.credentials(credentials);
        }

        if (StringUtils.hasText(config.baseUrl())) {
            HttpOptions httpOptions = HttpOptions.builder()
                    .baseUrl(config.baseUrl())
                    .build();
            builder.httpOptions(httpOptions);
        }

        return builder.build();
    }

    private GoogleCredentials buildGoogleCredentials(GeminiGenAiConfig config) {
        if (!config.useVertexAi()) {
            return null;
        }

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

    public record GeminiGenAiConfig(String apiKey,
                                    String projectId,
                                    String region,
                                    String model,
                                    String baseUrl,
                                    String serviceAccountJson,
                                    String oauthClientId,
                                    String oauthClientSecret,
                                    String oauthRefreshToken,
                                    boolean useVertexAi) implements ResourceConfig {

        @Override
        public String getResourceIdentifier() {
            if (useVertexAi) {
                return "vertex:" + projectId + ":" + region + ":" + model;
            }
            String baseUrlKey = StringUtils.hasText(baseUrl) ? baseUrl : "default";
            return "api_key:" + model + ":" + apiKey.hashCode() + ":" + baseUrlKey;
        }

        @Override
        public Map<String, Object> getContextMap() {
            Map<String, Object> context = new java.util.HashMap<>();
            context.put("mode", useVertexAi ? "vertex" : "api_key");
            context.put("model", model);
            context.put("credential_source", getCredentialSource());
            if (StringUtils.hasText(projectId)) {
                context.put("project_id", projectId);
            }
            if (StringUtils.hasText(region)) {
                context.put("region", region);
            }
            if (StringUtils.hasText(baseUrl)) {
                context.put("base_url", baseUrl);
            }
            return context;
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
