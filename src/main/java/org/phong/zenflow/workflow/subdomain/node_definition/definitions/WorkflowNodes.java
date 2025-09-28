package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Container for workflow nodes that provides efficient map-based lookup
 * while still serializing as a list for backwards compatibility.
 */
public class WorkflowNodes implements Serializable {
    private final Map<String, BaseWorkflowNode> nodeMap;

    public WorkflowNodes() {
        this.nodeMap = new LinkedHashMap<>();
    }

    @JsonCreator
    public WorkflowNodes(List<BaseWorkflowNode> nodes) {
        this();
        if (nodes != null) {
            nodes.forEach(n -> this.nodeMap.put(n.getKey(), n));
        }
    }

    /**
     * Deep copy constructor
     */
    public WorkflowNodes(WorkflowNodes other) {
        this();
        if (other != null) {
            other.nodeMap.forEach((key, node) -> this.nodeMap.put(key, new BaseWorkflowNode(node)));
        }
    }

    public BaseWorkflowNode get(String key) {
        return nodeMap.get(key);
    }

    public void put(BaseWorkflowNode node) {
        if (node != null) {
            nodeMap.put(node.getKey(), node);
        }
    }

    public void remove(String key) {
        nodeMap.remove(key);
    }

    public void clear() {
        nodeMap.clear();
    }

    @JsonIgnore
    public Map<String, BaseWorkflowNode> asMap() {
        return nodeMap;
    }

    @JsonIgnore
    public Set<Entry<String, BaseWorkflowNode>> entrySet() {
        return nodeMap.entrySet();
    }

    public void forEach(BiConsumer<String, BaseWorkflowNode> action) {
        nodeMap.forEach(action);
    }

    @JsonIgnore
    public Collection<BaseWorkflowNode> values() {
        return nodeMap.values();
    }

    @JsonIgnore
    public List<BaseWorkflowNode> toList() {
        return new ArrayList<>(nodeMap.values());
    }

    @JsonIgnore
    public Set<String> keys() {
        return nodeMap.keySet();
    }

    public WorkflowNodes deepCopy() {
        return new WorkflowNodes(this);
    }

    public BaseWorkflowNode findByNodeId(UUID nodeId) {
        if (nodeId == null) {
            return null;
        }

        for (Entry<String, BaseWorkflowNode> node : nodeMap.entrySet()) {
            if (nodeId.equals(node.getValue().getPluginNode().getNodeId())) {
                return node.getValue();
            }
        }

        return null;
    }

    public BaseWorkflowNode findByInstanceKey(String nodeKey) {
        if (nodeKey == null) {
            return null;
        }

        return nodeMap.get(nodeKey);
    }

    @JsonValue
    public List<BaseWorkflowNode> jsonValue() {
        return new ArrayList<>(nodeMap.values());
    }

    @JsonIgnore
    public Map<String, BaseWorkflowNode> getNodeMapGroupByNodeId() {
        Map<String, BaseWorkflowNode> grouped = new HashMap<>(nodeMap.size());
        nodeMap.forEach((key, node) -> grouped.put(node.getPluginNode().getNodeId().toString(), node));
        return grouped;
    }

    @JsonIgnore
    public Set<String> getPluginNodeCompositeKeys() {
        Set<String> keys = new HashSet<>(nodeMap.size());
        nodeMap.forEach((key, node) -> keys.add(node.getPluginNode().toCacheKey()));
        return keys;
    }

    @JsonIgnore
    public Set<UUID> getPluginNodeIds() {
        Set<UUID> ids = new HashSet<>(nodeMap.size());
        nodeMap.forEach((key, node) -> ids.add(node.getPluginNode().getNodeId()));
        return ids;
    }
}
