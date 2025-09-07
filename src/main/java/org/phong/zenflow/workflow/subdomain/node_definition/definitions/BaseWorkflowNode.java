package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class BaseWorkflowNode {
    @NotNull
    private String key;

    @NotNull
    private NodeType type;

    @NotNull
    private PluginNodeIdentifier pluginNode;

    @NotNull
    private List<String> next;

    @NotNull
    private WorkflowConfig config;

    private Map<String, Object> metadata;

    private Map<String, Object> policy;

    /**
     * TRUE Deep copy constructor - creates independent copies of all mutable objects
     */
    public BaseWorkflowNode(BaseWorkflowNode other) {
        this.key = other.key; // String is immutable, safe to share
        this.type = other.type; // Enum is immutable, safe to share

        // Deep copy PluginNodeIdentifier
        this.pluginNode = other.pluginNode != null ?
            new PluginNodeIdentifier(
                other.pluginNode.getNodeId(), // UUID is immutable
                other.pluginNode.getPluginKey(), // String is immutable
                other.pluginNode.getNodeKey(), // String is immutable
                other.pluginNode.getVersion(), // String is immutable
                other.pluginNode.getExecutorType() // String is immutable
            ) : null;

        // Deep copy next list
        this.next = other.next != null ? new ArrayList<>(other.next) : null;

        // Deep copy WorkflowConfig
        this.config = other.config != null ? deepCopyWorkflowConfig(other.config) : null;

        // Deep copy metadata map and its values
        this.metadata = other.metadata != null ? deepCopyMap(other.metadata) : null;

        // Deep copy policy map and its values
        this.policy = other.policy != null ? deepCopyMap(other.policy) : null;
    }

    /**
     * Deep copy a WorkflowConfig record
     */
    private WorkflowConfig deepCopyWorkflowConfig(WorkflowConfig config) {
        if (config == null) return null;

        return new WorkflowConfig(
            config.input() != null ? deepCopyMap(config.input()) : null,
            config.output() != null ? deepCopyMap(config.output()) : null,
            config.profile() != null ? deepCopyMap(config.profile()) : null
        );
    }

    /**
     * Deep copy a Map and all its values recursively
     */
    private Map<String, Object> deepCopyMap(Map<String, Object> original) {
        if (original == null) return null;

        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    /**
     * Deep copy any object value recursively
     */
    @SuppressWarnings("unchecked")
    private Object deepCopyValue(Object value) {
        if (value == null) return null;

        // Immutable types - safe to share
        if (value instanceof String || value instanceof Number ||
            value instanceof Boolean || value.getClass().isEnum()) {
            return value;
        }

        // Lists - recursively copy contents
        if (value instanceof List) {
            List<Object> originalList = (List<Object>) value;
            List<Object> copyList = new ArrayList<>();
            for (Object item : originalList) {
                copyList.add(deepCopyValue(item));
            }
            return copyList;
        }

        // Maps - recursively copy contents
        if (value instanceof Map) {
            Map<String, Object> originalMap = (Map<String, Object>) value;
            return deepCopyMap(originalMap);
        }

        // For other complex objects, you might need to implement custom deep copy logic
        // For now, log a warning and return the original (shallow copy)
        log.warn("WARNING: Shallow copy for unsupported type: {}", value.getClass().getName());
        return value;
    }
}