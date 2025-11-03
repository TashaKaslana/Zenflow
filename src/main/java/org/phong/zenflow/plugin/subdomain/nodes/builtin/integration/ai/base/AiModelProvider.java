package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiModelCapabilities;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Base interface for AI model providers that can support tool calling and various response formats.
 * Abstract providers accept typed requests from cluster and return structured results.
 */
public interface AiModelProvider {
    
    /**
     * Get the chat model instance
     */
    ChatModel getChatModel();
    
    /**
     * Execute AI model with typed request from cluster.
     * Provider applies tools, options, and formats according to its capabilities.
     * 
     * @param request Typed execution request with messages, tools, options
     * @return Structured execution result with output, metadata, and parse status
     */
    AiExecutionResult execute(AiExecutionRequest request);
    
    /**
     * Get the provider name (e.g., "gemini", "openai", etc.)
     */
    String getProviderName();
    
    /**
     * Get provider capabilities for cluster negotiation
     */
    AiModelCapabilities getCapabilities();
}
