package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Capabilities metadata for AI model providers.
 * Allows cluster to negotiate features with abstract providers.
 */
@Value
@Builder
public class AiModelCapabilities {
    /**
     * Provider supports tool/function calling
     */
    boolean supportsTools;
    
    /**
     * Provider supports JSON mode (structured output)
     */
    boolean supportsJsonMode;
    
    /**
     * Provider supports streaming responses
     */
    boolean supportsStreaming;
    
    /**
     * Provider supports vision/image inputs
     */
    boolean supportsVision;
    
    /**
     * Provider supports system messages
     */
    boolean supportsSystemMessages;
    
    /**
     * Maximum context window size (tokens)
     */
    Integer maxContextTokens;
    
    /**
     * Maximum output tokens
     */
    Integer maxOutputTokens;
}
