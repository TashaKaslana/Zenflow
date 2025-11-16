package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiExecutionContextKeys;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiClusterConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.factory.AiExecutionRequestFactory;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster.AiClusterProviderResolver.ProviderInfo;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.util.WorkflowNodeKeyUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
    private final AiClusterProviderResolver providerResolver;

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Starting AI cluster execution");

        try {
            // Read cluster configuration from context
            AiClusterConfig config = buildClusterConfig(context);
            ProviderInfo providerDescriptor = resolveProviderDescriptor(context, config.getModel());
            logs.info("Cluster config: provider={}, includeHistory={}, memoryKey={}", 
                    providerDescriptor.childAlias(), config.isIncludeHistory(), config.getMemoryKey());

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
            writeTypedRequest(context, request, logs);

            AiExecutionResult result = executeProvider(context, providerDescriptor, request, logs);
            logs.success("Provider execution completed: {}", result.getProvider());

            // Save conversation turn to memory if enabled
            if (config.isIncludeHistory() && config.getMemoryKey() != null) {
                saveConversationTurn(context, config.getMemoryKey(), request, result, logs);
                logs.info("Saved conversation turn to memory");
            }

            writeOutputs(context, result);

            logs.success("AI cluster execution completed successfully");
            return ExecutionResult.success();

        } catch (Exception e) {
            logs.error("AI cluster execution failed: {}", e.getMessage());
            log.error("AI cluster execution error", e);
            return ExecutionResult.error(ExecutionError.NON_RETRIABLE, "AI cluster execution failed: " + e.getMessage());
        } finally {
            cleanupTypedRequest(context, logs);
        }
    }

    private void writeTypedRequest(ExecutionContext context, AiExecutionRequest request, NodeLogPublisher logs) {
        try {
            context.write(AiExecutionContextKeys.TYPED_REQUEST, request);
        } catch (Exception ex) {
            logs.warn("Unable to stash typed AI execution request for provider: {}", ex.getMessage());
        }
    }

    private void cleanupTypedRequest(ExecutionContext context, NodeLogPublisher logs) {
        try {
            if (context.containsKey(AiExecutionContextKeys.TYPED_REQUEST)) {
                context.remove(AiExecutionContextKeys.TYPED_REQUEST);
            }
        } catch (Exception ex) {
            logs.warn("Failed to clear typed AI execution request from context: {}", ex.getMessage());
        }
    }

    private void writeOutputs(ExecutionContext context, AiExecutionResult result) {
        context.write("response", result.getOutput());
        context.write("raw_response", result.getRawResponse());
        context.write("provider", result.getProvider());
        context.write("parse_success", result.isParseSuccess());
        writeMetadata(context, result.getMetadata());
    }

    private void writeMetadata(ExecutionContext context, Map<String, Object> metadata) {
        try {
            context.remove("metadata");
        } catch (Exception ignored) {
            // If no metadata was previously stored, ignore.
        }
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        Object usage = metadata.get(AiExecutionContextKeys.USAGE_KEY);
        if (usage != null) {
            context.write(AiExecutionContextKeys.USAGE_KEY, usage);
        }
        metadata.forEach((key, value) -> {
            if (AiExecutionContextKeys.USAGE_KEY.equals(key)) {
                return;
            }
            context.write(AiExecutionContextKeys.METADATA_PREFIX + key, value);
        });
    }

    /**
     * Build cluster configuration from context inputs
     */
    private AiClusterConfig buildClusterConfig(ExecutionContext context) {
        String prompt = context.read("prompt", String.class);
        String systemPrompt = context.readOrDefault("system_prompt", String.class, null);
        String responseFormat = context.readOrDefault("response_format", String.class, "text");
        
        // Provider determines which abstract node to call (gemini, openai, etc.)
        String provider = context.readOrDefault("provider", String.class, null);
        
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

    private ProviderInfo resolveProviderDescriptor(ExecutionContext context, String requestedProvider) {
        if (requestedProvider != null && !requestedProvider.isBlank()) {
            return providerResolver.resolve(requestedProvider)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported model: " + requestedProvider));
        }
        return detectProviderFromChildren(context)
                .orElseGet(providerResolver::defaultProvider);
    }

    private Optional<ProviderInfo> detectProviderFromChildren(ExecutionContext context) {
        BaseWorkflowNode currentNode = context.getWorkflowNode(context.getNodeKey());
        if (currentNode == null || currentNode.getChildNodeKeys() == null || currentNode.getChildNodeKeys().isEmpty()) {
            return Optional.empty();
        }
        return currentNode.getChildNodeKeys().stream()
                .map(WorkflowNodeKeyUtils::extractChildAlias)
                .filter(Objects::nonNull)
                .map(providerResolver::resolveByChildAlias)
                .flatMap(Optional::stream)
                .findFirst();
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
                initializeMessage(messageMap, messages);
            } else {
                // Handle cases where an item might be serialized JSON string
                try {
                    Map<?, ?> messageMap = objectMapper.readValue(item.toString(), Map.class);
                    initializeMessage(messageMap, messages);
                } catch (Exception e) {
                    log.warn("Failed to parse message item: {}", item, e);
                }
            }
        }
        return messages;
    }

    private static void initializeMessage(Map<?, ?> messageMap, List<Message> messages) {
        String role = (String) messageMap.get("role");
        String content = (String) messageMap.get("content");

        if ("user".equals(role)) {
            messages.add(new UserMessage(content));
        } else if ("assistant".equals(role)) {
            messages.add(new AssistantMessage(content));
        }
    }

    /**
     * Execute abstract provider node with typed request
     */
    private AiExecutionResult executeProvider(ExecutionContext context, ProviderInfo descriptor,
                                             AiExecutionRequest request, NodeLogPublisher logs) {
        String providerKey = descriptor.pluginKey() + ":" + descriptor.nodeKey() + ":" + descriptor.version();
        logs.info("Executing provider node: {}", providerKey);

        // Build provider config - pass through the actual prompt and options
        // Provider will use the existing AiExecutor logic
        Map<String, Object> providerInput = new HashMap<>();
        extractLatestUserMessage(request)
            .ifPresent(prompt -> providerInput.put("prompt", prompt));
        providerInput.put("response_format", request.getResponseFormat());
        providerInput.put("model_options", request.getModelOptions());

        extractSystemPrompt(request).ifPresent(systemPrompt -> providerInput.put("system_prompt", systemPrompt));
        
        WorkflowConfig providerConfig = new WorkflowConfig(providerInput);

        // Execute provider via orchestrator
        logs.info("Calling provider via orchestrator: {}", providerKey);
        ExecutionResult providerResult;
        String childKey = WorkflowNodeKeyUtils.buildChildKey(context.getNodeKey(), descriptor.childAlias());
        BaseWorkflowNode providerNode = context.getWorkflowNode(childKey);
        if (providerNode != null) {
            logs.info("Executing materialized provider node: {}", childKey);
            providerNode.setConfig(providerConfig);
            providerResult = orchestrator.executeNode(providerNode, providerConfig, context);
        } else {
            logs.warn("Provider node '{}' not found in workflow definition, falling back to synthetic execution", childKey);
            providerResult = orchestrator.executeSyntheticNodeByKey(
                    providerKey,
                    "ai_provider",
                    providerConfig,
                    context
            );
        }

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
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMetadata = context.read("metadata", Map.class);
            if (contextMetadata != null) {
                metadata.putAll(contextMetadata);
            }
        } catch (Exception ignored) {
            // Metadata optional
        }
        try {
            Object usageObj = context.read("usage", Object.class);
            if (usageObj instanceof Map) {
                metadata.put("usage", usageObj);
            }
        } catch (Exception ignored) {
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

    // mapModelToProviderKey removed - registry handles provider resolution

    private void saveConversationTurn(ExecutionContext context,
                                      String memoryKey,
                                      AiExecutionRequest request,
                                      AiExecutionResult result,
                                      NodeLogPublisher logs) {
        try {
            extractLatestUserMessage(request)
                    .filter(message -> !message.isBlank())
                    .ifPresent(message -> appendToHistory(
                            context,
                            memoryKey,
                            buildMessageEntry("user", message, buildUserMetadata(request)),
                            logs));

            String assistantContent = determineAssistantContent(result);
            if (assistantContent != null && !assistantContent.isBlank()) {
                appendToHistory(context,
                        memoryKey,
                        buildMessageEntry("assistant", assistantContent, buildAssistantMetadata(result)),
                        logs);
            }

        } catch (Exception e) {
            logs.warn("Failed to save conversation turn: {}", e.getMessage());
        }
    }

    private void appendToHistory(ExecutionContext context,
                                 String memoryKey,
                                 Map<String, Object> entry,
                                 NodeLogPublisher logs) {
        if (entry == null || entry.get("content") == null || entry.get("content").toString().isBlank()) {
            logs.debug("Skipping conversation append due to empty content for key {}", memoryKey);
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("operation", "APPEND");
        payload.put("key", memoryKey);
        payload.put("value", entry);
        payload.put("persistent", true);

        orchestrator.executeSyntheticNodeByKey(
                "core:context_variable:1.0.0",
                "append_message",
                new WorkflowConfig(payload),
                context
        );
    }

    private Map<String, Object> buildMessageEntry(String role, String content, Map<String, Object> metadata) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", role);
        entry.put("content", content);
        entry.put("timestamp", Instant.now().toString());
        if (metadata != null && !metadata.isEmpty()) {
            entry.put("metadata", metadata);
        }
        return entry;
    }

    private Map<String, Object> buildUserMetadata(AiExecutionRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (request.getToolObjects() != null) {
            metadata.put("tool_count", request.getToolObjects().size());
        }
        if (request.getContextMetadata() != null && !request.getContextMetadata().isEmpty()) {
            metadata.put("context", request.getContextMetadata());
        }
        return metadata;
    }

    private Map<String, Object> buildAssistantMetadata(AiExecutionResult result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", result.getProvider());
        metadata.put("parse_success", result.isParseSuccess());
        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            metadata.putAll(result.getMetadata());
        }
        return metadata;
    }

    private String determineAssistantContent(AiExecutionResult result) {
        if (result.getRawResponse() != null && !result.getRawResponse().isBlank()) {
            return result.getRawResponse();
        }
        Object output = result.getOutput();
        return output != null ? output.toString() : null;
    }

    private Optional<String> extractLatestUserMessage(AiExecutionRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            return Optional.empty();
        }
        List<Message> messages = request.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message candidate = messages.get(i);
            if (candidate instanceof UserMessage userMessage) {
                return Optional.of(userMessage.getText());
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractSystemPrompt(AiExecutionRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            return Optional.empty();
        }
        return request.getMessages().stream()
                .filter(SystemMessage.class::isInstance)
                .map(message -> ((SystemMessage) message).getText())
                .findFirst();
    }
}
