package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiModelProvider;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;

import java.util.Map;

@Slf4j
public class GeminiModelProvider implements AiModelProvider {
    
    private final VertexAiGeminiChatModel chatModel;
    
    public GeminiModelProvider(VertexAiGeminiChatModel chatModel, 
                              AiToolRegistry toolRegistry,
                              AiObservationRegistry observationRegistry) {
        this.chatModel = chatModel;
    }

    @Override
    public ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public ChatResponse call(Prompt prompt, Map<String, Object> options) {
        // Build Gemini-specific options
        VertexAiGeminiChatOptions.Builder optionsBuilder = VertexAiGeminiChatOptions.builder();
        
        if (options != null && !options.isEmpty()) {
            // Temperature
            if (options.containsKey("temperature")) {
                Object temp = options.get("temperature");
                if (temp instanceof Number) {
                    optionsBuilder.temperature(((Number) temp).doubleValue());
                }
            }
            
            // Max output tokens
            if (options.containsKey("max_tokens")) {
                Object maxTokens = options.get("max_tokens");
                if (maxTokens instanceof Number) {
                    optionsBuilder.maxOutputTokens(((Number) maxTokens).intValue());
                }
            }
            
            // Top P
            if (options.containsKey("top_p")) {
                Object topP = options.get("top_p");
                if (topP instanceof Number) {
                    optionsBuilder.topP(((Number) topP).doubleValue());
                }
            }
            
            // Top K
            if (options.containsKey("top_k")) {
                Object topK = options.get("top_k");
                if (topK instanceof Number) {
                    optionsBuilder.topK(((Number) topK).intValue());
                }
            }

            // Model name
            if (options.containsKey("model")) {
                Object model = options.get("model");
                if (model instanceof String) {
                    optionsBuilder.model((String) model);
                }
            }
        }

        VertexAiGeminiChatOptions chatOptions = optionsBuilder.build();
        
        log.debug("Calling Gemini with options: temperature={}, maxTokens={}", 
            chatOptions.getTemperature(), chatOptions.getMaxOutputTokens());
        
        return chatModel.call(new Prompt(prompt.getInstructions(), chatOptions));
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }
}
