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
    
    private final ObjectMapper objectMapper;

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
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.observationRegistry = observationRegistry;
        log.info("AiExecutor initialized with {} tools and observation registry", toolRegistry.size());
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Starting AI execution");

        try {
            // Get the model provider from the factory
            AiModelProvider modelProvider = modelProviderFactory.apply(context);
            logs.info("Using AI provider: {}", modelProvider.getProviderName());

            // Read configuration from context (direct node usage, not cluster)
            String userPrompt = context.read("prompt", String.class);
            String systemPrompt = context.readOrDefault("system_prompt", String.class, null);
            String responseFormat = context.readOrDefault("response_format", String.class, "text");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> modelOptions = context.readOrDefault("model_options", Map.class, new HashMap<>());

            // Build messages
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            messages.add(new UserMessage(userPrompt));

            logs.info("Sending request to AI model with {} messages", messages.size());

            // Build typed request
            AiExecutionRequest request = AiExecutionRequest.builder()
                    .messages(messages)
                    .modelOptions(modelOptions)
                    .toolObjects(toolRegistry.getToolList())
                    .responseFormat(responseFormat)
                    .contextMetadata(new HashMap<>())
                    .build();

            // Execute through provider
            AiExecutionResult result = modelProvider.execute(request);
            
            logs.success("Received response from AI model");

            // Write results to context
            context.write("response", result.getOutput());
            context.write("raw_response", result.getRawResponse());
            context.write("provider", result.getProvider());
            context.write("parse_success", result.isParseSuccess());
            
            // Write metadata
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

}
