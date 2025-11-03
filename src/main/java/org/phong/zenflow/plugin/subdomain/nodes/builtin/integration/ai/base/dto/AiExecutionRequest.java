package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

/**
 * Typed request for AI model execution.
 * Built by cluster and passed to abstract provider nodes.
 */
@Value
@Builder
public class AiExecutionRequest {
    /**
     * Conversation messages (system, user, assistant, history)
     */
    List<Message> messages;
    
    /**
     * Model-specific configuration options (validated by provider)
     */
    Map<String, Object> modelOptions;
    
    /**
     * Tool handles registered for this execution
     */
    List<Object> toolObjects;
    
    /**
     * Response format hint ("text", "json", etc.)
     */
    String responseFormat;
    
    /**
     * Conversation context metadata (optional)
     */
    Map<String, Object> contextMetadata;
}
