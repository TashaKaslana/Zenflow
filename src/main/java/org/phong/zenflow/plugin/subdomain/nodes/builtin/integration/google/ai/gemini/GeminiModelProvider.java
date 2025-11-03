package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiModelProvider;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiModelCapabilities;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;

import java.util.*;

@Slf4j
public class GeminiModelProvider implements AiModelProvider {
    
    private final VertexAiGeminiChatModel chatModel;
    private final AiToolRegistry toolRegistry;
    private final AiObservationRegistry observationRegistry;
    private final ObjectMapper objectMapper;
    
    public GeminiModelProvider(
            VertexAiGeminiChatModel chatModel,
            AiToolRegistry toolRegistry,
            AiObservationRegistry observationRegistry,
            ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.toolRegistry = toolRegistry;
        this.observationRegistry = observationRegistry;
        this.objectMapper = objectMapper;
        
        log.info("GeminiModelProvider initialized with {} tools", toolRegistry.size());
    }

    @Override
    public ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public AiExecutionResult execute(AiExecutionRequest request) {
        log.debug("Executing Gemini with {} messages, {} tools, format: {}",
                request.getMessages().size(), 
                request.getToolObjects().size(),
                request.getResponseFormat());
        
        // Build Gemini-specific options
        VertexAiGeminiChatOptions.Builder optionsBuilder = VertexAiGeminiChatOptions.builder();
        
        // Apply model options
        applyModelOptions(optionsBuilder, request.getModelOptions());
        
        // TODO: Wire tools from registry (requires proper Spring AI function callback integration)
        if (!request.getToolObjects().isEmpty()) {
            log.debug("Tools registered but not yet wired to Gemini (tool support coming in next phase): {} tools", 
                    request.getToolObjects().size());
        }
        
        VertexAiGeminiChatOptions chatOptions = optionsBuilder.build();
        
        log.debug("Calling Gemini with options: temperature={}, maxTokens={}", 
                chatOptions.getTemperature(), 
                chatOptions.getMaxOutputTokens());
        
        // Execute model call
        Prompt prompt = new Prompt(request.getMessages(), chatOptions);
        ChatResponse response = chatModel.call(prompt);
        
        // Extract response text
        String rawResponse = response.getResult().getOutput().getText();
        log.debug("Received response from Gemini: {} chars", rawResponse.length());
        
        // Parse response according to format
        Object parsedOutput;
        boolean parseSuccess = true;
        
        if ("json".equalsIgnoreCase(request.getResponseFormat())) {
            try {
                parsedOutput = objectMapper.readValue(rawResponse, Object.class);
                log.debug("Successfully parsed response as JSON");
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse response as JSON, returning raw text: {}", e.getMessage());
                parsedOutput = rawResponse;
                parseSuccess = false;
            }
        } else {
            parsedOutput = rawResponse;
        }
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            metadata.put("usage", Map.of(
                    "prompt_tokens", response.getMetadata().getUsage().getPromptTokens(),
                    "completion_tokens", response.getMetadata().getUsage().getCompletionTokens(),
                    "total_tokens", response.getMetadata().getUsage().getTotalTokens()
            ));
        }
        metadata.put("model", chatOptions.getModel());
        
        return AiExecutionResult.builder()
                .output(parsedOutput)
                .rawResponse(rawResponse)
                .provider(getProviderName())
                .metadata(metadata)
                .parseSuccess(parseSuccess)
                .build();
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public AiModelCapabilities getCapabilities() {
        return AiModelCapabilities.builder()
                .supportsTools(true)  // Capability exists, wiring in progress
                .supportsJsonMode(true)
                .supportsStreaming(true)
                .supportsVision(true)
                .supportsSystemMessages(true)
                .maxContextTokens(1_000_000)  // Gemini 1.5 Pro has 1M context
                .maxOutputTokens(8192)
                .build();
    }
    
    private void applyModelOptions(VertexAiGeminiChatOptions.Builder builder, Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        
        // Temperature
        if (options.containsKey("temperature")) {
            Object temp = options.get("temperature");
            if (temp instanceof Number) {
                builder.temperature(((Number) temp).doubleValue());
            }
        }
        
        // Max output tokens
        if (options.containsKey("max_tokens")) {
            Object maxTokens = options.get("max_tokens");
            if (maxTokens instanceof Number) {
                builder.maxOutputTokens(((Number) maxTokens).intValue());
            }
        }
        
        // Top P
        if (options.containsKey("top_p")) {
            Object topP = options.get("top_p");
            if (topP instanceof Number) {
                builder.topP(((Number) topP).doubleValue());
            }
        }
        
        // Top K
        if (options.containsKey("top_k")) {
            Object topK = options.get("top_k");
            if (topK instanceof Number) {
                builder.topK(((Number) topK).intValue());
            }
        }

        // Model name
        if (options.containsKey("model")) {
            Object model = options.get("model");
            if (model instanceof String) {
                builder.model((String) model);
            }
        }
    }
}


