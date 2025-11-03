package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiClusterConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.factory.AiExecutionRequestFactory;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI Cluster executor - orchestrates AI execution with memory, tools, and multi-model support.
 * Delegates to abstract provider nodes (Gemini, OpenAI, etc.) via NodeExecutionOrchestrator.
 */
@Component
@AllArgsConstructor
@Slf4j
public class AiClusterExecutor implements NodeExecutor {
    
    private final NodeExecutionOrchestrator orchestrator;
    private final AiExecutionRequestFactory requestFactory;
    private final AiToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Starting AI cluster execution");

        try {
            // Read cluster configuration from context
            AiClusterConfig config = buildClusterConfig(context);
            logs.info("Cluster config: model={}, includeHistory={}, memoryKey={}", 
                    config.getModel(), config.isIncludeHistory(), config.getMemoryKey());

            // Retrieve conversation history if enabled
            List<Message> conversationHistory = new ArrayList<>();
            if (config.isIncludeHistory() && config.getMemoryKey() != null) {
                conversationHistory = retrieveConversationHistory(context, config.getMemoryKey(), logs);
                logs.info("Retrieved {} messages from conversation history", conversationHistory.size());
            }

            // Build typed execution request
            AiExecutionRequest request = requestFactory.build(config, toolRegistry, conversationHistory);
            logs.info("Built execution request with {} messages, {} tools", 
                    request.getMessages().size(), request.getToolObjects().size());

            // Execute abstract provider node
            AiExecutionResult result = executeProvider(context, config.getModel(), request, logs);
            logs.success("Provider execution completed: {}", result.getProvider());

            // Save conversation turn to memory if enabled
            if (config.isIncludeHistory() && config.getMemoryKey() != null) {
                saveConversationTurn(context, config.getMemoryKey(), config.getPrompt(), result.getRawResponse(), logs);
                logs.info("Saved conversation turn to memory");
            }

            // Write results to context
            context.write("response", result.getOutput());
            context.write("raw_response", result.getRawResponse());
            context.write("provider", result.getProvider());
            context.write("parse_success", result.isParseSuccess());
            
            // Write metadata
            if (result.getMetadata() != null) {
                context.write("metadata", result.getMetadata());
            }

            logs.success("AI cluster execution completed successfully");
            return ExecutionResult.success();

        } catch (Exception e) {
            logs.error("AI cluster execution failed: {}", e.getMessage());
            log.error("AI cluster execution error", e);
            return ExecutionResult.error(ExecutionError.NON_RETRIABLE, "AI cluster execution failed: " + e.getMessage());
        }
    }

    /**
     * Build cluster configuration from context inputs
     */
    private AiClusterConfig buildClusterConfig(ExecutionContext context) {
        String prompt = context.read("prompt", String.class);
        String systemPrompt = context.readOrDefault("system_prompt", String.class, null);
        String responseFormat = context.readOrDefault("response_format", String.class, "text");
        
        // Provider determines which abstract node to call (gemini, openai, etc.)
        String provider = context.readOrDefault("provider", String.class, "gemini");
        
        // Model is the specific variant (optional, goes into model_options)
        String modelVariant = context.readOrDefault("model", String.class, null);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> modelOptionsInput = context.readOrDefault("model_options", Map.class, new HashMap<>());
        
        Map<String, Object> modelOptions = new HashMap<>(modelOptionsInput);
        
        // If specific model variant provided, add it to model_options
        if (modelVariant != null && !modelVariant.isEmpty()) {
            modelOptions.put("model", modelVariant);
        }
        
        String memoryKey = context.readOrDefault("memory_key", String.class, null);
        Boolean includeHistory = context.readOrDefault("include_history", Boolean.class, false);
        Integer maxHistoryMessages = context.readOrDefault("max_history_messages", Integer.class, 10);

        return AiClusterConfig.builder()
                .prompt(prompt)
                .systemPrompt(systemPrompt)
                .responseFormat(responseFormat)
                .model(provider)  // Model field in config actually means provider
                .modelOptions(modelOptions)
                .memoryKey(memoryKey)
                .includeHistory(includeHistory)
                .maxHistoryMessages(maxHistoryMessages)
                .build();
    }

    /**
     * Retrieve conversation history from memory node
     */
    @SuppressWarnings("unchecked")
    private List<Message> retrieveConversationHistory(ExecutionContext context, String memoryKey, NodeLogPublisher logs) {
        try {
            // Call memory node to retrieve history
            WorkflowConfig retrieveConfig = new WorkflowConfig(Map.of(
                    "operation", "RETRIEVE",
                    "key", memoryKey,
                    "default_value", List.of()
            ));

            ExecutionResult result = orchestrator.executeSyntheticNodeByKey(
                    "core:context_variable:1.0.0",
                    "retrieve_history",
                    retrieveConfig,
                    context
            );

            if (result.getStatus() == ExecutionStatus.SUCCESS) {
                Object historyResult = context.read("result", Object.class);
                if (historyResult instanceof Map) {
                    Object value = ((Map<String, Object>) historyResult).get("value");
                    if (value instanceof List) {
                        return convertToMessages((List<?>) value);
                    }
                }
            }

            logs.warn("Failed to retrieve conversation history from memory key: {}", memoryKey);
            return new ArrayList<>();

        } catch (Exception e) {
            logs.warn("Error retrieving conversation history: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convert stored history objects to Spring AI Message objects
     */
    private List<Message> convertToMessages(List<?> historyList) {
        List<Message> messages = new ArrayList<>();
        for (Object item : historyList) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageMap = (Map<String, Object>) item;
                String role = (String) messageMap.get("role");
                String content = (String) messageMap.get("content");
                
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            } else {
                // Handle cases where item might be serialized JSON string
                try {
                    Map<?, ?> messageMap = objectMapper.readValue(item.toString(), Map.class);
                    String role = (String) messageMap.get("role");
                    String content = (String) messageMap.get("content");
                    
                    if ("user".equals(role)) {
                        messages.add(new UserMessage(content));
                    } else if ("assistant".equals(role)) {
                        messages.add(new AssistantMessage(content));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse message item: {}", item, e);
                }
            }
        }
        return messages;
    }

    /**
     * Execute abstract provider node with typed request
     */
    private AiExecutionResult executeProvider(ExecutionContext context, String model, 
                                             AiExecutionRequest request, NodeLogPublisher logs) {
        // Map model identifier to plugin node key
        String providerKey = mapModelToProviderKey(model);
        logs.info("Executing provider node: {}", providerKey);

        // Validate provider is supported
        if (!providerKey.equals("google-ai:gemini:1.0.0")) {
            throw new IllegalArgumentException("Unsupported model: " + model + " (only google-ai:gemini supported currently)");
        }

        // Build provider config - pass through the actual prompt and options
        // Provider will use the existing AiExecutor logic
        Map<String, Object> providerInput = new HashMap<>();
        providerInput.put("prompt", request.getMessages().get(request.getMessages().size() - 1).getText());
        providerInput.put("response_format", request.getResponseFormat());
        providerInput.put("model_options", request.getModelOptions());
        
        // Add system prompt if present in messages
        if (!request.getMessages().isEmpty() && 
            request.getMessages().get(0) instanceof org.springframework.ai.chat.messages.SystemMessage) {
            providerInput.put("system_prompt", request.getMessages().get(0).getText());
        }
        
        WorkflowConfig providerConfig = new WorkflowConfig(providerInput);

        // Execute provider via orchestrator
        logs.info("Calling provider via orchestrator: {}", providerKey);
        ExecutionResult providerResult = orchestrator.executeSyntheticNodeByKey(
                providerKey,
                "ai_provider",
                providerConfig,
                context
        );

        // Check execution status
        if (providerResult.getStatus() != ExecutionStatus.SUCCESS) {
            String errorMsg = providerResult.getError() != null ? providerResult.getError() : "Unknown error";
            throw new RuntimeException("Provider execution failed: " + errorMsg);
        }

        // Read provider outputs from context
        Object response = context.read("response", Object.class);
        String rawResponse = context.read("raw_response", String.class);
        String provider = context.read("provider", String.class);
        
        // Read metadata if available
        Map<String, Object> metadata = new HashMap<>();
        try {
            Object usageObj = context.read("usage", Object.class);
            if (usageObj instanceof Map) {
                metadata.put("usage", usageObj);
            }
        } catch (Exception e) {
            // Usage metadata optional
        }

        // Determine parse success (if response format is JSON, check if response is parsed)
        boolean parseSuccess = true;
        if ("json".equalsIgnoreCase(request.getResponseFormat())) {
            parseSuccess = (response instanceof Map || response instanceof List);
        }

        logs.info("Provider call completed: {} chars, parseSuccess={}", 
                rawResponse != null ? rawResponse.length() : 0, parseSuccess);

        return AiExecutionResult.builder()
                .output(response)
                .rawResponse(rawResponse)
                .provider(provider)
                .metadata(metadata)
                .parseSuccess(parseSuccess)
                .build();
    }

    /**
     * Map user-friendly model name to plugin node composite key
     */
    private String mapModelToProviderKey(String model) {
        // Map model identifier to plugin node key
        return switch (model.toLowerCase()) {
            case "gemini", "google-ai:gemini", "gemini-2.0-flash", "gemini-1.5-pro" -> "google-ai:gemini:1.0.0";
            case "openai:gpt-4", "gpt-4" -> "openai:gpt-4:1.0.0"; // Future
            case "openai:gpt-3.5", "gpt-3.5-turbo" -> "openai:gpt-3.5:1.0.0"; // Future
            default -> throw new IllegalArgumentException("Unknown model: " + model);
        };
    }

    /**
     * Save conversation turn (user + assistant) to memory
     */
    private void saveConversationTurn(ExecutionContext context, String memoryKey, 
                                     String userMessage, String assistantMessage, NodeLogPublisher logs) {
        try {
            // Append user message
            appendToHistory(context, memoryKey, "user", userMessage);
            
            // Append assistant message
            appendToHistory(context, memoryKey, "assistant", assistantMessage);
            
        } catch (Exception e) {
            logs.warn("Failed to save conversation turn: {}", e.getMessage());
        }
    }

    /**
     * Append a message to conversation history
     */
    private void appendToHistory(ExecutionContext context, String memoryKey, String role, String content) {
        WorkflowConfig appendConfig = new WorkflowConfig(Map.of(
                "operation", "APPEND",
                "key", memoryKey,
                "value", Map.of(
                        "role", role,
                        "content", content
                )
        ));

        orchestrator.executeSyntheticNodeByKey(
                "core:context_variable:1.0.0",
                "append_message",
                appendConfig,
                context
        );
    }
}
