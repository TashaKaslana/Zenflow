package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * Base executor for AI nodes with tool calling and observation support.
 * Each AI model (Gemini, OpenAI, etc.) should copy registries for isolation.
 */
@Component
@Slf4j
public class AiExecutor implements NodeExecutor {
    
    private final ObjectMapper objectMapper;
    private final AiToolRegistry toolRegistry;
    private final AiObservationRegistry observationRegistry;
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

    public AiObservationRegistry getObservationRegistry() {
        return observationRegistry;
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Starting AI execution");

        try {
            // Get the model provider from the factory
            AiModelProvider modelProvider = modelProviderFactory.apply(context);
            logs.info("Using AI provider: {}", modelProvider.getProviderName());

            // Read configuration
            String userPrompt = context.read("prompt", String.class);
            String systemPrompt = context.readOrDefault("system_prompt", String.class, null);
            String responseFormat = context.readOrDefault("response_format", String.class, "text"); // "text" or "json"
            
            @SuppressWarnings("unchecked")
            Map<String, Object> modelOptions = context.readOrDefault("model_options", Map.class, new HashMap<>());

            // Build messages
            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            messages.add(new UserMessage(userPrompt));

            Prompt prompt = new Prompt(messages);
            logs.info("Sending request to AI model with {} messages", messages.size());

            // Call the model
            ChatResponse response = modelProvider.call(prompt, modelOptions);
            
            // Get response text from AssistantMessage
            String responseContent = response.getResult().getOutput().getText();
            logs.success("Received response from AI model");

            // Process response based on format
            Object processedResponse;
            if ("json".equalsIgnoreCase(responseFormat)) {
                try {
                    processedResponse = objectMapper.readValue(responseContent, Object.class);
                    logs.info("Parsed response as JSON");
                } catch (JsonProcessingException e) {
                    logs.warn("Failed to parse response as JSON, returning as text: {}", e.getMessage());
                    processedResponse = responseContent;
                }
            } else {
                processedResponse = responseContent;
            }

            // Write results to context
            context.write("response", processedResponse);
            context.write("raw_response", responseContent);
            context.write("provider", modelProvider.getProviderName());
            
            // Write metadata if available
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                context.write("usage", Map.of(
                    "prompt_tokens", response.getMetadata().getUsage().getPromptTokens(),
                    "completion_tokens", response.getMetadata().getUsage().getCompletionTokens(),
                    "total_tokens", response.getMetadata().getUsage().getTotalTokens()
                ));
            }

            return ExecutionResult.success();

        } catch (Exception e) {
            logs.error("AI execution failed: {}", e.getMessage());
            log.error("AI execution error", e);
            return ExecutionResult.error(ExecutionError.NON_RETRIABLE, "AI execution failed: " + e.getMessage());
        }
    }

    /**
     * Set the model provider factory
     */
    public void setModelProviderFactory(Function<ExecutionContext, AiModelProvider> factory) {
        this.modelProviderFactory = factory;
    }
    
    /**
     * Get the tool registry for registering custom AI tools/functions
     */
    public AiToolRegistry getToolRegistry() {
        return toolRegistry;
    }
}
