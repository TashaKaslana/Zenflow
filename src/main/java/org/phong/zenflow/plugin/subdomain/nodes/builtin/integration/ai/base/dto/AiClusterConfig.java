package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto;

import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools.ToolRouterConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.memory.MemoryConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.parser.ParserConfig;

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

    /**
     * Optional override map for compound child nodes (tools, context, parser).
     * Values should use the <pluginKey>:<nodeKey>:<version> format.
     */
    @Builder.Default
    Map<String, String> childNodes = Map.of();

    /**
     * Optional override map for child executor types keyed by child alias.
     * When absent, defaults to the executor type defined by the cluster.
     */
    @Builder.Default
    Map<String, String> childExecutorTypes = Map.of();

    /**
     * Tool routing configuration.
     */
    @Builder.Default
    ToolRouterConfig toolRouterConfig = ToolRouterConfig.builder().build();

    /**
     * Memory configuration.
     */
    @Builder.Default
    MemoryConfig memoryConfig = MemoryConfig.builder().build();

    /**
     * Parser configuration.
     */
    @Builder.Default
    ParserConfig parserConfig = ParserConfig.builder().build();

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

        Map<String, String> childNodes = extractStringMap(input.get("child_nodes"));
        Map<String, String> childExecutorTypes = extractStringMap(input.get("child_executor_types"));
        ToolRouterConfig toolConfig = ToolRouterConfig.fromRaw(input.get("tools"));
        MemoryConfig memoryConfig = MemoryConfig.fromRaw(input.get("memory"));
        ParserConfig parserConfig = ParserConfig.fromRaw(input.get("parser"));

        return AiClusterConfig.builder()
                .prompt(prompt)
                .systemPrompt(systemPrompt)
                .responseFormat(responseFormat)
                .model(provider)
                .modelOptions(modelOptions)
                .memoryKey(memoryKey)
                .includeHistory(includeHistory)
                .maxHistoryMessages(maxHistoryMessages)
                .childNodes(childNodes)
                .childExecutorTypes(childExecutorTypes)
                .toolRouterConfig(toolConfig)
                .memoryConfig(memoryConfig)
                .parserConfig(parserConfig)
                .build();
    }

    private static Map<String, String> extractStringMap(Object raw) {
        if (!(raw instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() != null) {
                result.put(key, entry.getValue().toString());
            }
        }
        return Map.copyOf(result);
    }
}
