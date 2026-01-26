package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

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
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GeminiModelProvider implements AiModelProvider {

    private final GoogleGenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public GeminiModelProvider(GoogleGenAiChatModel chatModel,
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
        String responseFormat = request.getResponseFormat();
        if (responseFormat == null || responseFormat.isBlank()) {
            responseFormat = "text";
        }
        log.debug("Executing Gemini API with {} messages, {} tools, format: {}",
                request.getMessages().size(),
                request.getToolObjects().size(),
                responseFormat);

        GoogleGenAiChatOptions chatOptions = buildOptions(request);
        Prompt prompt = new Prompt(request.getMessages(), chatOptions);
        ChatResponse response = chatModel.call(prompt);

        String rawResponse = response.getResult().getOutput().getText();
        log.debug("Received response from Gemini API: {} chars", rawResponse != null ? rawResponse.length() : null);

        Object parsedOutput = rawResponse;
        boolean parseSuccess = true;
        if ("json".equalsIgnoreCase(responseFormat)) {
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
        return "gemini";
    }

    @Override
    public AiModelCapabilities getCapabilities() {
        return AiModelCapabilities.builder()
                .supportsTools(true)
                .supportsJsonMode(true)
                .supportsStreaming(true)
                .supportsVision(true)
                .supportsSystemMessages(true)
                .maxContextTokens(1_000_000)
                .maxOutputTokens(8192)
                .build();
    }

    private GoogleGenAiChatOptions buildOptions(AiExecutionRequest request) {
        GoogleGenAiChatOptions options;
        if (chatModel.getDefaultOptions() instanceof GoogleGenAiChatOptions defaultOptions) {
            options = GoogleGenAiChatOptions.fromOptions(defaultOptions);
        } else {
            options = GoogleGenAiChatOptions.builder().build();
        }

        applyModelOptions(options, request.getModelOptions(), request.getToolObjects());

        if ("json".equalsIgnoreCase(request.getResponseFormat())) {
            options.setResponseMimeType("application/json");
        }

        return options;
    }

    private void applyModelOptions(GoogleGenAiChatOptions options, Map<String, Object> modelOptions, List<Object> toolObjects) {
        if (toolObjects != null) {
            List<ToolCallback> tools = toolObjects.stream().map(t -> (ToolCallback) t).toList();
            options.setToolCallbacks(tools);
        }

        if (modelOptions == null || modelOptions.isEmpty()) {
            return;
        }

        Object temperature = modelOptions.get("temperature");
        if (temperature instanceof Number temp) {
            options.setTemperature(temp.doubleValue());
        }

        Object maxTokens = modelOptions.get("max_tokens");
        if (maxTokens instanceof Number max) {
            options.setMaxOutputTokens(max.intValue());
        }

        Object maxOutputTokens = modelOptions.get("max_output_tokens");
        if (maxOutputTokens instanceof Number max) {
            options.setMaxOutputTokens(max.intValue());
        }

        Object topP = modelOptions.get("top_p");
        if (topP instanceof Number top) {
            options.setTopP(top.doubleValue());
        }

        Object topK = modelOptions.get("top_k");
        if (topK instanceof Number top) {
            options.setTopK(top.intValue());
        }

        Object frequencyPenalty = modelOptions.get("frequency_penalty");
        if (frequencyPenalty instanceof Number freq) {
            options.setFrequencyPenalty(freq.doubleValue());
        }

        Object presencePenalty = modelOptions.get("presence_penalty");
        if (presencePenalty instanceof Number presence) {
            options.setPresencePenalty(presence.doubleValue());
        }

        Object candidateCount = modelOptions.get("candidate_count");
        if (candidateCount instanceof Number candidates) {
            options.setCandidateCount(candidates.intValue());
        }

        Object modelOverride = modelOptions.get("model");
        if (modelOverride instanceof String modelName && !modelName.isBlank()) {
            options.setModel(modelName);
        }

        Object responseMimeType = modelOptions.get("response_mime_type");
        if (responseMimeType instanceof String mimeType && !mimeType.isBlank()) {
            options.setResponseMimeType(mimeType);
        }
    }
}
