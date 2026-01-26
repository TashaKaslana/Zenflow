package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openai.chatgpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiModelProvider;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiModelCapabilities;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ChatgptModelProvider implements AiModelProvider {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public ChatgptModelProvider(OpenAiChatModel chatModel,
                               ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public AiExecutionResult execute(AiExecutionRequest request) {
        log.debug("Executing OpenAi API with {} messages, {} tools, format: {}",
                request.getMessages().size(),
                request.getToolObjects().size(),
                request.getResponseFormat());

        OpenAiChatOptions.Builder optionsBuilder;
        if (chatModel.getDefaultOptions() instanceof OpenAiChatOptions defaultOptions) {
            optionsBuilder = new OpenAiChatOptions.Builder(OpenAiChatOptions.fromOptions(defaultOptions));
        } else {
            optionsBuilder = OpenAiChatOptions.builder();
        }
        applyModelOptions(optionsBuilder, request.getModelOptions(), request.getToolObjects());

        if ("json".equalsIgnoreCase(request.getResponseFormat())) {
            optionsBuilder.responseFormat(ResponseFormat.builder()
                    .type(ResponseFormat.Type.JSON_OBJECT)
                    .build());
        }

        OpenAiChatOptions chatOptions = optionsBuilder.build();

        Prompt prompt = new Prompt(request.getMessages(), chatOptions);
        ChatResponse response = chatModel.call(prompt);

        String rawResponse = response.getResult().getOutput().getText();
        log.debug("Received response from OpenAi API: {} chars", rawResponse != null ? rawResponse.length() : null);

        Object parsedOutput = rawResponse;
        boolean parseSuccess = true;
        if ("json".equalsIgnoreCase(request.getResponseFormat())) {
            try {
                parsedOutput = objectMapper.readValue(rawResponse, Object.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse response as JSON, returning raw text: {}", e.getMessage());
                parseSuccess = false;
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        if (response.getMetadata().getUsage() != null) {
            metadata.put("usage", Map.of(
                    "prompt_tokens", response.getMetadata().getUsage().getPromptTokens(),
                    "completion_tokens", response.getMetadata().getUsage().getCompletionTokens(),
                    "total_tokens", response.getMetadata().getUsage().getTotalTokens()
            ));
        }
        if (chatOptions.getModel() != null) {
            metadata.put("model", chatOptions.getModel());
        }

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
        return "openai";
    }

    @Override
    public AiModelCapabilities getCapabilities() {
        return AiModelCapabilities.builder()
                .supportsTools(true)
                .supportsJsonMode(true)
                .supportsStreaming(false)
                .supportsVision(false)
                .supportsSystemMessages(true)
                .maxContextTokens(128_000)
                .maxOutputTokens(16_384)
                .build();
    }

    private void applyModelOptions(OpenAiChatOptions.Builder builder, Map<String, Object> options, List<Object> toolObjects) {
        if (toolObjects != null) {
            List<ToolCallback> tools = toolObjects.stream().map(t -> (ToolCallback) t).toList();
            builder.toolCallbacks(tools);
        }

        if (options == null || options.isEmpty()) {
            return;
        }

        Object temperature = options.get("temperature");
        if (temperature instanceof Number temp) {
            builder.temperature(temp.doubleValue());
        }

        Object maxTokens = options.get("max_tokens");
        if (maxTokens instanceof Number max) {
            builder.maxTokens(max.intValue());
        }

        Object topP = options.get("top_p");
        if (topP instanceof Number top) {
            builder.topP(top.doubleValue());
        }

        Object frequencyPenalty = options.get("frequency_penalty");
        if (frequencyPenalty instanceof Number freq) {
            builder.frequencyPenalty(freq.doubleValue());
        }

        Object presencePenalty = options.get("presence_penalty");
        if (presencePenalty instanceof Number presence) {
            builder.presencePenalty(presence.doubleValue());
        }

        Object modelOverride = options.get("model");
        if (modelOverride instanceof String modelName && !modelName.isBlank()) {
            builder.model(modelName);
        }

        Object responseFormat = options.get("response_format");
        if (responseFormat instanceof Map<?, ?> formatMap) {
            ResponseFormat.Builder rfBuilder = ResponseFormat.builder();
            Object type = formatMap.get("type");
            if (type instanceof String typeStr) {
                try {
                    rfBuilder.type(ResponseFormat.Type.valueOf(typeStr.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown response_format.type '{}', ignoring", typeStr);
                }
            }
            builder.responseFormat(rfBuilder.build());
        }
    }
}
