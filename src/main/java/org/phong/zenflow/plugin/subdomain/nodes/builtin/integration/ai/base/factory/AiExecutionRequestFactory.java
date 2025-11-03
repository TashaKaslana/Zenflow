package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.factory;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiClusterConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for building AI execution requests from cluster config.
 * Assembles messages, tools, and options for provider execution.
 */
@Component
@Slf4j
public class AiExecutionRequestFactory {
    
    /**
     * Build an execution request from cluster config and conversation context.
     *
     * @param config Cluster configuration
     * @param toolRegistry Tool registry for this execution
     * @param conversationHistory Optional conversation history messages
     * @return Typed execution request
     */
    public AiExecutionRequest build(
            AiClusterConfig config,
            AiToolRegistry toolRegistry,
            List<Message> conversationHistory) {
        
        List<Message> messages = new ArrayList<>();
        
        // Add system prompt if provided
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isEmpty()) {
            messages.add(new SystemMessage(config.getSystemPrompt()));
            log.debug("Added system message");
        }
        
        // Add conversation history if provided
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            int historyCount = Math.min(conversationHistory.size(), config.getMaxHistoryMessages());
            List<Message> limitedHistory = conversationHistory.subList(
                    Math.max(0, conversationHistory.size() - historyCount),
                    conversationHistory.size()
            );
            messages.addAll(limitedHistory);
            log.debug("Added {} history messages (limited from {})", limitedHistory.size(), conversationHistory.size());
        }
        
        // Add current user prompt
        messages.add(new UserMessage(config.getPrompt()));
        log.debug("Added user message");
        
        // Build model options
        Map<String, Object> modelOptions = config.getModelOptions() != null
                ? new HashMap<>(config.getModelOptions())
                : new HashMap<>();
        
        // Build context metadata
        Map<String, Object> contextMetadata = new HashMap<>();
        if (config.getMemoryKey() != null) {
            contextMetadata.put("memory_key", config.getMemoryKey());
        }
        contextMetadata.put("include_history", config.isIncludeHistory());
        
        log.info("Built AI execution request with {} messages, {} tools, response format: {}",
                messages.size(), toolRegistry.size(), config.getResponseFormat());
        
        return AiExecutionRequest.builder()
                .messages(messages)
                .modelOptions(modelOptions)
                .toolObjects(toolRegistry.getToolList())
                .responseFormat(config.getResponseFormat())
                .contextMetadata(contextMetadata)
                .build();
    }
}
