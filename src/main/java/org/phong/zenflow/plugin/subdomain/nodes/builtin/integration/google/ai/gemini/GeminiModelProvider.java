package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiModelProvider;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiModelCapabilities;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GeminiModelProvider implements AiModelProvider {

    private final GeminiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public GeminiModelProvider(GeminiChatModel chatModel,
                               ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatModel getChatModel() {
        // Custom Gemini implementation does not expose a Spring ChatModel adapter yet.
        return null;
    }

    @Override
    public AiExecutionResult execute(AiExecutionRequest request) {
        List<Message> messages = request.getMessages() != null ? request.getMessages() : List.of();
        List<Object> tools = request.getToolObjects() != null ? request.getToolObjects() : List.of();
        String responseFormat = request.getResponseFormat();
        if (responseFormat == null || responseFormat.isBlank()) {
            responseFormat = "text";
        }
        log.debug("Executing Gemini API with {} messages, {} tools, format: {}",
                messages.size(),
                tools.size(),
                responseFormat);

        if (!tools.isEmpty()) {
            log.warn("Gemini API (direct) does not yet support tool-calling; {} tool(s) ignored", tools.size());
        }

        String prompt = buildPrompt(messages);
        GeminiChatModel.GeminiResponse response = chatModel.call(prompt);
        String rawResponse = response != null ? response.getText() : null;
        log.debug("Received response from Gemini API: {} chars", rawResponse != null ? rawResponse.length() : null);

        Object parsedOutput = rawResponse;
        boolean parseSuccess = rawResponse != null;
        if ("json".equalsIgnoreCase(responseFormat) && rawResponse != null) {
            try {
                parsedOutput = objectMapper.readValue(rawResponse, Object.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse response as JSON, returning raw text: {}", e.getMessage());
                parseSuccess = false;
            }
        }

        Map<String, Object> metadata = buildMetadata(response);

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
                .supportsTools(false)
                .supportsJsonMode(true)
                .supportsStreaming(false)
                .supportsVision(false)
                .supportsSystemMessages(true)
                .maxContextTokens(1_000_000)
                .maxOutputTokens(8192)
                .build();
    }

    private String buildPrompt(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Message message : messages) {
            String content = message.getText();
            if (content == null) {
                content = "";
            }
            builder.append(message.getMessageType().name().toLowerCase())
                    .append(": ")
                    .append(content)
                    .append("\n");
        }

        return builder.toString().trim();
    }

    private Map<String, Object> buildMetadata(GeminiChatModel.GeminiResponse response) {
        Map<String, Object> metadata = new HashMap<>();
        if (response == null) {
            return metadata;
        }

        if (response.getPromptTokens() > 0
                || response.getOutputTokens() > 0
                || response.getTotalTokens() > 0) {
            Map<String, Object> usage = new HashMap<>();
            usage.put("prompt_tokens", response.getPromptTokens());
            usage.put("completion_tokens", response.getOutputTokens());
            usage.put("total_tokens", response.getTotalTokens());
            metadata.put("usage", usage);
        }

        if (response.getFinishReason() != null) {
            metadata.put("finish_reason", response.getFinishReason());
        }
        if (response.getResponseId() != null) {
            metadata.put("response_id", response.getResponseId());
        }
        if (response.getModelVersion() != null) {
            metadata.put("model_version", response.getModelVersion());
        }

        return metadata;
    }
}
