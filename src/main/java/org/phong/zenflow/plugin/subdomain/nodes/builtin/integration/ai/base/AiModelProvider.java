package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;

/**
 * Base interface for AI model providers that can support tool calling and various response formats
 */
public interface AiModelProvider {
    
    /**
     * Get the chat model instance
     */
    ChatModel getChatModel();
    
    /**
     * Generate a chat response from the model
     * 
     * @param prompt The prompt to send to the model
     * @param options Additional options for the model (temperature, max tokens, etc.)
     * @return The chat response
     */
    ChatResponse call(Prompt prompt, Map<String, Object> options);
    
    /**
     * Get the provider name (e.g., "gemini", "openai", etc.)
     */
    String getProviderName();
}
