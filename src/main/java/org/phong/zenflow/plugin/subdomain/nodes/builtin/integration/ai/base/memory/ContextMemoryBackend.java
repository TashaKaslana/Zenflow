package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;

import java.util.Collections;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default memory backend that relies on the context_variable node for storage.
 */
@RequiredArgsConstructor
@Slf4j
public class ContextMemoryBackend implements MemoryBackend {

    private final ExecutionContext context;
    private final ObjectMapper objectMapper;
    private final MemoryConfig config;
    private final NodeLogPublisher logs;

    @Override
    public List<Message> loadHistory() {
        String memoryKey = config.getKey();
        if (memoryKey == null || memoryKey.isBlank()) {
            return List.of();
        }
        try {
            WorkflowConfig retrieveConfig = new WorkflowConfig(Map.of(
                    "operation", "RETRIEVE",
                    "key", memoryKey,
                    "default_value", List.of()
            ));

            ExecutionResult result = context.executeSubNode(
                    "core:context_variable:1.0.0",
                    retrieveConfig
            );

            if (result.getStatus() == ExecutionStatus.SUCCESS) {
                Map<String, Object> outputs = result.getOutputPayload() != null ? result.getOutputPayload() : Collections.emptyMap();
                Object historyResult = outputs.get("result");
                if (historyResult instanceof Map<?, ?> historyMap) {
                    Object value = historyMap.get("value");
                    if (value instanceof List<?> list) {
                        return convertToMessages(list);
                    }
                }
            }

            logs.warn("Failed to retrieve conversation history from memory key: {}", memoryKey);
            return List.of();

        } catch (Exception e) {
            logs.warn("Error retrieving conversation history: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void appendTurn(Map<String, Object> entry) {
        String memoryKey = config.getKey();
        if (memoryKey == null || memoryKey.isBlank()) {
            return;
        }
        if (entry == null || entry.get("content") == null || entry.get("content").toString().isBlank()) {
            logs.debug("Skipping conversation append due to empty content for key {}", memoryKey);
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("operation", "APPEND");
        payload.put("key", memoryKey);
        payload.put("value", entry);
        payload.put("persistent", config.isPersistent());

        context.executeSubNode(
                "core:context_variable:1.0.0",
                new WorkflowConfig(payload)
        );
    }

    private List<Message> convertToMessages(List<?> historyList) {
        List<Message> messages = new ArrayList<>();
        for (Object item : historyList) {
            if (item instanceof Map<?, ?> messageMap) {
                initializeMessage(messageMap, messages);
            } else {
                try {
                    Map<?, ?> messageMap = objectMapper.readValue(item.toString(), Map.class);
                    initializeMessage(messageMap, messages);
                } catch (Exception e) {
                    log.warn("Failed to parse message item: {}", item, e);
                }
            }
        }
        // Trim to max history
        int max = Math.max(1, config.getMaxHistoryMessages());
        if (messages.size() > max) {
            return new ArrayList<>(messages.subList(messages.size() - max, messages.size()));
        }
        return messages;
    }

    private static void initializeMessage(Map<?, ?> messageMap, List<Message> messages) {
        Object roleObj = messageMap.get("role");
        Object contentObj = messageMap.get("content");
        if (roleObj == null || contentObj == null) {
            return;
        }
        String role = Objects.toString(roleObj, "");
        String content = Objects.toString(contentObj, "");

        if ("user".equals(role)) {
            messages.add(new UserMessage(content));
        } else if ("assistant".equals(role)) {
            messages.add(new AssistantMessage(content));
        } else if ("system".equals(role)) {
            messages.add(new SystemMessage(content));
        }
    }

    /**
    * Helper to create standard entries with timestamp.
    */
    public static Map<String, Object> buildMessageEntry(String role, String content, Map<String, Object> metadata) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", role);
        entry.put("content", content);
        entry.put("timestamp", Instant.now().toString());
        if (metadata != null && !metadata.isEmpty()) {
            entry.put("metadata", metadata);
        }
        return entry;
    }
}
