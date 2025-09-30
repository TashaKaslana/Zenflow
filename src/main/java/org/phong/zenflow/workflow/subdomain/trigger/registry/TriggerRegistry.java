package org.phong.zenflow.workflow.subdomain.trigger.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor
@Slf4j
@Component
public class TriggerRegistry {
    private final PluginNodeExecutorRegistry registry;
    private final Set<String> triggerExecutorIds = new HashSet<>();

    // Map from trigger key to TriggerType for efficient lookup
    private final Map<String, TriggerType> triggerTypeMap = new ConcurrentHashMap<>();

    // Map from TriggerType to list of trigger keys
    private final Map<TriggerType, List<String>> typeToTriggersMap = new ConcurrentHashMap<>();

    public void registerTrigger(String key, Class<?> nodeClass) {
        triggerExecutorIds.add(key);

        PluginNode annotation = nodeClass.getAnnotation(PluginNode.class);
        if (annotation == null || !"trigger".equals(annotation.type())) {
            return;
        }

        String triggerTypeStr = annotation.triggerType();
        if (triggerTypeStr.isEmpty()) {
            return;
        }

        try {
            TriggerType triggerType = mapStringToTriggerType(triggerTypeStr);

            triggerTypeMap.put(key, triggerType);
            typeToTriggersMap.computeIfAbsent(triggerType, k -> new ArrayList<>()).add(key);

            log.debug("Registered trigger {} with type {}", key, triggerType);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown trigger type '{}' for trigger {}, skipping type mapping", triggerTypeStr, key);
        }
    }

    public void unregisterTrigger(String key) {
        triggerExecutorIds.remove(key);

        // Clean up type mappings
        TriggerType triggerType = triggerTypeMap.remove(key);
        if (triggerType != null) {
            List<String> triggers = typeToTriggersMap.get(triggerType);
            if (triggers != null) {
                triggers.remove(key);
                if (triggers.isEmpty()) {
                    typeToTriggersMap.remove(triggerType);
                }
            }
        }
    }

    public TriggerExecutor getRegistry(String triggerKey) {
        Optional<NodeDefinition> definition = registry.getDefinition(triggerKey);
        if (definition.isEmpty()) {
            if (!triggerExecutorIds.contains(triggerKey)) {
                log.warn("Trigger not found for key {}", triggerKey);
            }
            return null;
        }

        NodeExecutor executor = definition.get().getNodeExecutor();
        if (executor instanceof TriggerExecutor triggerExecutor) {
            return triggerExecutor;
        }

        log.warn("Registered trigger {} does not expose a TriggerExecutor", triggerKey);
        return null;
    }

    public Set<String> getAllTriggerKeys() {
        return triggerExecutorIds;
    }

    /**
     * Get the TriggerType for a specific trigger key
     */
    public Optional<TriggerType> getTriggerType(String triggerKey) {
        return Optional.ofNullable(triggerTypeMap.get(triggerKey));
    }

    /**
     * Get all trigger keys of a specific type
     */
    public List<String> getTriggersByType(TriggerType triggerType) {
        return typeToTriggersMap.getOrDefault(triggerType, Collections.emptyList());
    }

    /**
     * Check if a trigger key corresponds to a specific trigger type
     */
    public boolean isTriggerOfType(String triggerKey, TriggerType triggerType) {
        return triggerType.equals(triggerTypeMap.get(triggerKey));
    }

    /**
     * Get all available trigger types that have registered triggers
     */
    public Set<TriggerType> getAvailableTriggerTypes() {
        return typeToTriggersMap.keySet();
    }

    /**
     * Maps string trigger type from annotation to TriggerType enum
     */
    private TriggerType mapStringToTriggerType(String triggerTypeStr) {
        return switch (triggerTypeStr.toLowerCase()) {
            case "manual" -> TriggerType.MANUAL;
            case "webhook" -> TriggerType.WEBHOOK;
            case "event" -> TriggerType.EVENT;
            case "schedule" -> TriggerType.SCHEDULE;
            case "polling" -> TriggerType.POLLING;
            default -> throw new IllegalArgumentException("Unknown trigger type: " + triggerTypeStr);
        };
    }
}
