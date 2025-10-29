package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.core.GcpCredentialsProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class GeminiResourceManager extends BaseNodeResourceManager<VertexAiGeminiChatModel, GeminiResourceManager.GeminiConfig> {

    private final AiObservationRegistry observationRegistry;

    @Override
    public GeminiConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        // Read from profile - simple API key pattern
        String apiKey = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.API_KEY);
        String projectId = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.PROJECT_ID);
        String region = (String) ctx.getProfileSecret(GcpCredentialsProfileDescriptor.REGION);
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API_KEY not found in profile. Please configure an AI credentials profile.");
        }
        
        // For Gemini, we need project ID (can be extracted from API key or provided separately)
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("PROJECT_ID not found in profile. Gemini requires a GCP project ID.");
        }
        
        // Region defaults to us-central1 if not provided
        if (region == null || region.isBlank()) {
            region = "us-central1";
        }
        
        // Read model from node config (optional, defaults to gemini-2.0-flash)
        String model = ctx.readOrDefault("model", String.class, "gemini-2.0-flash");
        
        return new GeminiConfig(apiKey, projectId, region, model);
    }

    @Override
    protected VertexAiGeminiChatModel createResource(String resourceKey, GeminiConfig config) {
        log.info("Creating new Gemini chat model for key: {}", resourceKey);
        
        // Create credentials from API key (access token pattern)
        GoogleCredentials credentials = GoogleCredentials.create(
            new AccessToken(config.getApiKey(), new Date(System.currentTimeMillis() + 3600000))
        );
        
        // Build VertexAI client with credentials
        VertexAI vertexAI = new VertexAI.Builder()
                .setProjectId(config.getProjectId())
                .setLocation(config.getRegion())
                .setCredentials(credentials)
                .build();
        
        // Build chat options
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model(config.getModel())
                .temperature(0.7)
                .build();
        
        // Create and return chat model with Spring AI parameters
        return new VertexAiGeminiChatModel(
                vertexAI, 
                options, 
                null,
                null,
                observationRegistry.getRegistry()
        );
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

    /**
     * Configuration for Gemini resources.
     * Simple API key pattern - works like OpenAI SDK.
     */
    @Data
    public static class GeminiConfig implements ResourceConfig {
        private final String apiKey;
        private final String projectId;
        private final String region;
        private final String model;

        @Override
        public String getResourceIdentifier() {
            // Pool by project + region + model
            return projectId + ":" + region + ":" + model;
        }

        @Override
        public Map<String, Object> getContextMap() {
            return java.util.Map.of(
                    "project_id", projectId,
                    "region", region,
                    "model", model
            );
        }
    }
}
