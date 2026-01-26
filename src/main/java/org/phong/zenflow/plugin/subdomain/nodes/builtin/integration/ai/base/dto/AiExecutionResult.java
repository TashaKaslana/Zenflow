package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Result from AI model execution.
 * Returned by abstract provider to cluster.
 */
@Value
@Builder(toBuilder = true)
public class AiExecutionResult {
    /**
     * Processed output (parsed JSON or raw text)
     */
    Object output;
    
    /**
     * Raw response text from model
     */
    String rawResponse;
    
    /**
     * Provider name (e.g., "gemini", "openai")
     */
    String provider;
    
    /**
     * Execution metadata (token usage, cost, latency, etc.)
     */
    Map<String, Object> metadata;
    
    /**
     * Whether the response was successfully parsed according to requested format
     */
    boolean parseSuccess;
}
