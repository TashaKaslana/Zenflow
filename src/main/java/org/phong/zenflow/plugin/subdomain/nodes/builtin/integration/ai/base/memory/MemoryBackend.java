package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.memory;

import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

/**
 * Abstraction for conversation memory.
 */
public interface MemoryBackend {

    /**
     * Load conversation history (if present).
     */
    List<Message> loadHistory();

    /**
     * Append a turn to history.
     */
    void appendTurn(Map<String, Object> entry);
}
