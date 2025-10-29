package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import com.google.cloud.vertexai.VertexAI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
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
        String projectId = ctx.read("project_id", String.class);
        String location = ctx.readOrDefault("location", String.class, "us-central1");
        String model = ctx.readOrDefault("model", String.class, "gemini-2.0-flash");
        
        return new GeminiConfig(projectId, location, model);
    }

    @Override
    protected VertexAiGeminiChatModel createResource(String resourceKey, GeminiConfig config) {
        log.info("Creating new Gemini chat model for key: {}", resourceKey);
        
        // Create VertexAI client
        VertexAI vertexAI = new VertexAI.Builder()
                .setProjectId(config.getProjectId())
                .setLocation(config.getLocation())
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
     * Configuration for Gemini resources
     */
    @Data
    public static class GeminiConfig implements ResourceConfig {
        private final String projectId;
        private final String location;
        private final String model;

        @Override
        public String getResourceIdentifier() {
            return String.format("%s:%s:%s", projectId, location, model);
        }

        @Override
        public Map<String, Object> getContextMap() {
            return java.util.Map.of(
                    "project_id", projectId,
                    "location", location,
                    "model", model
            );
        }
    }
}
