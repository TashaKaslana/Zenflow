package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.util.WorkflowNodeKeyUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI Cluster executor - orchestrates AI execution with memory, tools, and multi-model support.
 * Delegates to abstract provider nodes (Gemini, OpenAI, etc.) via NodeExecutionOrchestrator.
 */
@Component
@Slf4j
public class AiClusterExecutor implements NodeExecutor {

    private final AiExecutionRequestFactory requestFactory;
    private final AiToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final AiClusterProviderResolver providerResolver;

    private final ParserStrategies parserStrategies;

    public AiClusterExecutor(AiExecutionRequestFactory requestFactory,
                             AiToolRegistry toolRegistry,
                             ObjectMapper objectMapper,
                             AiClusterProviderResolver providerResolver) {
        this.requestFactory = requestFactory;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.providerResolver = providerResolver;
        this.parserStrategies = new ParserStrategies(objectMapper);
    }

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
            MemoryBackend memoryBackend = buildMemoryBackend(config, context, logs);
            boolean isUsedMemory = config.isIncludeHistory() && config.getMemoryKey() != null && memoryBackend != null;
            if (isUsedMemory) {
                conversationHistory = memoryBackend.loadHistory();
                logs.info("Retrieved {} messages from conversation history", conversationHistory.size());
            }

            // Build tool list via router
            List<Object> tools = resolveTools(config, context);

            // Build typed execution request
            AiExecutionRequest request = requestFactory.build(config, tools, conversationHistory);
            logs.info("Built execution request with {} messages, {} tools", 
                    request.getMessages().size(), request.getToolObjects().size());

            // Execute abstract provider node
            writeTypedRequest(context, request, logs);

            String childAlias = resolveProviderChildAlias(context, providerDescriptor, config.getModel());
            AiExecutionResult result = executeProvider(context, providerDescriptor, childAlias, request, logs);
            logs.success("Provider execution completed: {}", result.getProvider());

            // Apply parser strategy
            AiExecutionResult parsedResult = applyParser(config, result, logs);

            // Save conversation turn to memory if enabled
            if (isUsedMemory) {
                saveConversationTurn(memoryBackend, request, parsedResult, logs);
                logs.info("Saved conversation turn to memory");
            }

            writeOutputs(context, parsedResult);

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

        @SuppressWarnings("unchecked")
        Map<String, Object> rawTools = context.readOrDefault("tools", Map.class, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> rawMemory = context.readOrDefault("memory", Map.class, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> rawParser = context.readOrDefault("parser", Map.class, new HashMap<>());

        ToolRouterConfig toolConfig = ToolRouterConfig.fromRaw(rawTools);
        MemoryConfig memoryConfig = MemoryConfig.fromRaw(rawMemory);
        if (memoryKey == null && memoryConfig.getKey() != null) {
            memoryKey = memoryConfig.getKey();
        }
        if (memoryKey != null) {
            memoryConfig = memoryConfig.toBuilder().key(memoryKey).build();
        }
        if (maxHistoryMessages != null) {
            memoryConfig = memoryConfig.toBuilder().maxHistoryMessages(maxHistoryMessages).build();
        }
        ParserConfig parserConfig = ParserConfig.fromRaw(rawParser);

        return AiClusterConfig.builder()
                .prompt(prompt)
                .systemPrompt(systemPrompt)
                .responseFormat(responseFormat)
                .model(provider)  // Model field in config actually means provider
                .modelOptions(modelOptions)
                .memoryKey(memoryKey)
                .includeHistory(includeHistory)
                .maxHistoryMessages(maxHistoryMessages)
                .toolRouterConfig(toolConfig)
                .memoryConfig(memoryConfig)
                .parserConfig(parserConfig)
                .build();
    }

    private List<Object> resolveTools(AiClusterConfig config, ExecutionContext context) {
        ToolRouter router = new DefaultToolRouter(
                toolRegistry.copy(), 
                config.getToolRouterConfig(),
                context,
                objectMapper
        );
        return router.resolveTools();
    }

    private MemoryBackend buildMemoryBackend(AiClusterConfig config, ExecutionContext context, NodeLogPublisher logs) {
        MemoryConfig mc = config.getMemoryConfig();
        if (mc == null) {
            return null;
        }
        return switch (mc.getBackend()) {
            case CONTEXT, IN_MEMORY -> new ContextMemoryBackend(context, objectMapper, mc, logs);
            case KV, VECTOR, EXTERNAL -> {
                logs.warn("Memory backend {} not implemented; falling back to context backend", mc.getBackend());
                yield new ContextMemoryBackend(context, objectMapper, mc, logs);
            }
        };
    }

    private AiExecutionResult applyParser(AiClusterConfig config, AiExecutionResult raw, NodeLogPublisher logs) {
        ParserConfig parserConfig = config.getParserConfig() != null ? config.getParserConfig() : ParserConfig.builder().build();
        ParserStrategy strategy = parserStrategies.forConfig(parserConfig);
        AiExecutionResult parsed = strategy.parse(raw);
        if (!parsed.isParseSuccess()) {
            logs.warn("Parser reported failure (strategy={}): output may be raw", parserConfig.getStrategy());
        }
        return parsed;
    }

    private ProviderInfo resolveProviderDescriptor(ExecutionContext context, String requestedProvider) {
        if (requestedProvider != null && !requestedProvider.isBlank()) {
            return providerResolver.resolve(requestedProvider)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported model: " + requestedProvider));
        }
        return detectProviderFromChildren(context)
                .orElseGet(providerResolver::defaultProvider);
    }

    private String resolveProviderChildAlias(ExecutionContext context, ProviderInfo providerInfo, String requestedProvider) {
        // 1. Check if the requested provider string matches an existing child node
        if (requestedProvider != null) {
            String requestedKey = WorkflowNodeKeyUtils.buildChildKey(context.getNodeKey(), requestedProvider);
            if (context.getWorkflowNode(requestedKey) != null) {
                return requestedProvider;
            }
        }

        // 2. Check if the canonical child alias exists
        String canonicalKey = WorkflowNodeKeyUtils.buildChildKey(context.getNodeKey(), providerInfo.childAlias());
        if (context.getWorkflowNode(canonicalKey) != null) {
            return providerInfo.childAlias();
        }

        // 3. Check if any other alias of the provider exists as a child
        for (String alias : providerInfo.aliases()) {
            String key = WorkflowNodeKeyUtils.buildChildKey(context.getNodeKey(), alias);
            if (context.getWorkflowNode(key) != null) {
                return alias;
            }
        }

        // 4. Fallback to canonical
        return providerInfo.childAlias();
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
     * Execute abstract provider node with typed request
     */
    private AiExecutionResult executeProvider(ExecutionContext context, ProviderInfo descriptor,
                                             String childAlias, AiExecutionRequest request, NodeLogPublisher logs) {
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
        String childKey = WorkflowNodeKeyUtils.buildChildKey(context.getNodeKey(), childAlias);
        BaseWorkflowNode providerNode = context.getWorkflowNode(childKey);
        if (providerNode != null) {
            logs.info("Executing materialized provider node: {}", childKey);
            providerNode.setConfig(providerConfig);
            providerResult = context.executeSubNode(providerNode, providerConfig);
        } else {
            logs.warn("Provider node '{}' not found in workflow definition, falling back to synthetic execution", childKey);
            providerResult = context.executeSubNode(providerKey, providerConfig);
        }

        // Check execution status
        if (providerResult.getStatus() != ExecutionStatus.SUCCESS) {
            String errorMsg = providerResult.getError() != null ? providerResult.getError() : "Unknown error";
            throw new RuntimeException("Provider execution failed: " + errorMsg);
        }

        Map<String, Object> outputs = providerResult.getOutputPayload() != null ? providerResult.getOutputPayload() : Collections.emptyMap();
        Object response = outputs.get("response");
        String rawResponse = (String) outputs.get("raw_response");
        String provider = (String) outputs.get("provider");
        
        // Read metadata if available
        Map<String, Object> metadata = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMetadata = (Map<String, Object>) outputs.get("metadata");
            if (contextMetadata != null) {
                metadata.putAll(contextMetadata);
            }
        } catch (Exception ignored) {
            // Metadata optional
        }
        try {
            Object usageObj = outputs.get("usage");
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

    private void saveConversationTurn(MemoryBackend memoryBackend,
                                      AiExecutionRequest request,
                                      AiExecutionResult result,
                                      NodeLogPublisher logs) {
        try {
            extractLatestUserMessage(request)
                    .filter(message -> !message.isBlank())
                    .ifPresent(message -> memoryBackend.appendTurn(
                            ContextMemoryBackend.buildMessageEntry("user", message, buildUserMetadata(request))));

            String assistantContent = determineAssistantContent(result);
            if (assistantContent != null && !assistantContent.isBlank()) {
                memoryBackend.appendTurn(ContextMemoryBackend.buildMessageEntry(
                        "assistant", assistantContent, buildAssistantMetadata(result)));
            }

        } catch (Exception e) {
            logs.warn("Failed to save conversation turn: {}", e.getMessage());
        }
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
