package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto;

import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

/**
 * Configuration for AI cluster node.
 * User-facing configuration that gets translated to AiExecutionRequest.
 */
@Value
@Builder
public class AiClusterConfig {
    /**
     * User prompt/message
     */
    String prompt;
    
    /**
     * Optional system prompt
     */
    String systemPrompt;
    
    /**
     * Response format (text, json)
     */
    @Builder.Default
    String responseFormat = "text";
    
    /**
     * Provider identifier (maps to abstract node key like "google-ai:gemini")
     * This field is named "model" for backwards compatibility but represents the provider
     */
    String model;
    
    /**
     * Model-specific options (including specific model variant like "gemini-2.0-flash")
     */
    Map<String, Object> modelOptions;
    
    /**
     * Memory/context key for conversation history
     */
    String memoryKey;
    
    /**
     * Whether to include conversation history
     */
    @Builder.Default
    boolean includeHistory = false;
    
    /**
     * Maximum history messages to include
     */
    @Builder.Default
    int maxHistoryMessages = 10;
    
    /**
     * Custom tools to register (in addition to platform defaults)
     */
    List<String> customTools;
    
    /**
     * Output parser strategy (json, text, custom)
     */
    @Builder.Default
    String outputParser = "auto";

    public static AiClusterConfig fromNodeConfig(WorkflowConfig workflowConfig) {
        Map<String, Object> input = workflowConfig != null ? workflowConfig.input() : Map.of();
        String prompt = (String) input.get("prompt");
        String systemPrompt = (String) input.getOrDefault("system_prompt", null);
        String responseFormat = (String) input.getOrDefault("response_format", "text");
        String provider = (String) input.getOrDefault("provider", "gemini");
        String modelVariant = (String) input.getOrDefault("model", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> modelOptions = input.containsKey("model_options") && input.get("model_options") instanceof Map
                ? new HashMap<>((Map<String, Object>) input.get("model_options"))
                : new HashMap<>();
        if (modelVariant != null && !modelVariant.isBlank()) {
            modelOptions.put("model", modelVariant);
        }
        String memoryKey = (String) input.getOrDefault("memory_key", null);
        boolean includeHistory = Boolean.TRUE.equals(input.get("include_history"));
        int maxHistoryMessages = input.containsKey("max_history_messages") && input.get("max_history_messages") instanceof Number
                ? ((Number) input.get("max_history_messages")).intValue()
                : 10;

        return AiClusterConfig.builder()
                .prompt(prompt)
                .systemPrompt(systemPrompt)
                .responseFormat(responseFormat)
                .model(provider)
                .modelOptions(modelOptions)
                .memoryKey(memoryKey)
                .includeHistory(includeHistory)
                .maxHistoryMessages(maxHistoryMessages)
                .build();
    }
}
