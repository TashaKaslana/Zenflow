package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowDefinition(List<BaseWorkflowNode> nodes, WorkflowMetadata metadata) implements Serializable {
    public WorkflowDefinition() {
        this(new ArrayList<>(), new WorkflowMetadata());
    }

    public WorkflowDefinition(WorkflowDefinition existingDef) {
        this(existingDef.nodes(), existingDef.metadata());
    }

    public WorkflowDefinition(List<BaseWorkflowNode> nodes, WorkflowMetadata metadata) {
        this.nodes = nodes != null ? nodes :new ArrayList<>();
        this.metadata = metadata != null ? metadata : new WorkflowMetadata();
    }

    /**
     * Creates a deep copy using manual copying (the fastest approach - 10-50x faster than Jackson)
     */
    public WorkflowDefinition deepCopy() {
        List<BaseWorkflowNode> copiedNodes = nodes != null ?
            nodes.stream()
                .map(BaseWorkflowNode::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll) :
            new ArrayList<>();

        WorkflowMetadata copiedMetadata = metadata != null ?
            new WorkflowMetadata(metadata) :
            new WorkflowMetadata();

        return new WorkflowDefinition(copiedNodes, copiedMetadata);
    }

    public Set<String> getNodeKeys() {
        return nodes.stream()
                .map(BaseWorkflowNode::getKey)
                .collect(Collectors.toSet());
    }

    public Map<String, BaseWorkflowNode> getNodeMap() {
        return nodes.stream()
                .collect(Collectors.toMap(
                        BaseWorkflowNode::getKey,
                        node -> node,
                        (existing, replacement) -> existing
                ));
    }

    public Set<String> getPluginNodeCompositeKeys() {
        return nodes.stream()
                .map(node -> node.getPluginNode().toCacheKey())
                .collect(Collectors.toSet());
    }

    public Set<UUID> getPluginNodeIds() {
        return nodes.stream()
                .map(node -> node.getPluginNode().getNodeId())
                .collect(Collectors.toSet());
    }
}
