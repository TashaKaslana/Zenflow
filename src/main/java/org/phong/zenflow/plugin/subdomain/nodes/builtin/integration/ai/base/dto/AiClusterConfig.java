package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

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
}
