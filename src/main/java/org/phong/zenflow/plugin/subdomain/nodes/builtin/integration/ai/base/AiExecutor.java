package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * Base executor for AI nodes - now acts as simple adapter for abstract providers.
 * Reads config from context, builds typed request, delegates to provider.
 */
@Component
@Slf4j
public class AiExecutor implements NodeExecutor {

    /**
     * -- GETTER --
     *  Get the tool registry for registering custom AI tools/functions
     */
    @Getter
    private final AiToolRegistry toolRegistry;
    private final AiObservationRegistry observationRegistry;

    /**
     * -- SETTER --
     *  Set the model provider factory
     */
    @Setter
    private Function<ExecutionContext, AiModelProvider> modelProviderFactory;

    /**
     * Spring injects base registries - models should copy() before use to ensure independence
     */
    public AiExecutor(ObjectMapper objectMapper, 
                      AiToolRegistry toolRegistry,
                      AiObservationRegistry observationRegistry) {
        this.toolRegistry = toolRegistry;
        this.observationRegistry = observationRegistry;
        log.info("AiExecutor initialized with {} tools (mapper={}, observation={})",
            toolRegistry.size(),
            objectMapper != null ? objectMapper.getClass().getSimpleName() : "unknown",
            observationRegistry != null ? observationRegistry.getClass().getSimpleName() : "unknown");
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Starting AI execution");

        try {
            AiModelProvider modelProvider = modelProviderFactory.apply(context);
            logs.info("Using AI provider: {}", modelProvider.getProviderName());

            AiExecutionRequest request = loadTypedRequest(context, logs);
            if (request == null) {
                request = buildRequestFromContext(context, logs);
            }

            AiExecutionResult result = modelProvider.execute(request);

            logs.success("Received response from AI model");

            context.write("response", result.getOutput());
            context.write("raw_response", result.getRawResponse());
            context.write("provider", result.getProvider());
            context.write("parse_success", result.isParseSuccess());

            if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
                result.getMetadata().forEach(context::write);
            }

            return ExecutionResult.success();

        } catch (Exception e) {
            logs.error("AI execution failed: {}", e.getMessage());
            log.error("AI execution error", e);
            return ExecutionResult.error(ExecutionError.NON_RETRIABLE, "AI execution failed: " + e.getMessage());
        }
    }

    private AiExecutionRequest buildRequestFromContext(ExecutionContext context, NodeLogPublisher logs) {
        String userPrompt = context.read("prompt", String.class);
        String systemPrompt = context.readOrDefault("system_prompt", String.class, null);
        String responseFormat = context.readOrDefault("response_format", String.class, "text");

        @SuppressWarnings("unchecked")
        Map<String, Object> modelOptions = context.readOrDefault("model_options", Map.class, new HashMap<>());

        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.add(new UserMessage(userPrompt));

        logs.info("Sending request to AI model with {} messages", messages.size());

        return AiExecutionRequest.builder()
                .messages(messages)
                .modelOptions(modelOptions)
                .toolObjects(toolRegistry.getToolList())
                .responseFormat(responseFormat)
                .contextMetadata(new HashMap<>())
                .build();
    }

    private AiExecutionRequest loadTypedRequest(ExecutionContext context, NodeLogPublisher logs) {
        try {
            if (!context.containsKey(AiExecutionContextKeys.TYPED_REQUEST)) {
                return null;
            }
            AiExecutionRequest request = context.read(AiExecutionContextKeys.TYPED_REQUEST, AiExecutionRequest.class);
            if (request != null) {
                logs.info("Using orchestrator-supplied typed AI execution request with {} messages and {} tools",
                        request.getMessages() != null ? request.getMessages().size() : 0,
                        request.getToolObjects() != null ? request.getToolObjects().size() : 0);
            } else {
                logs.warn("Typed AI execution request key present but payload missing");
            }
            return request;
        } catch (Exception ex) {
            logs.warn("Failed to read typed AI execution request: {}", ex.getMessage());
            return null;
        }
    }

}
