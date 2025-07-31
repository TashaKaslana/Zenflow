package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
     * Deep copy constructor
     */
    public BaseWorkflowNode(BaseWorkflowNode other) {
        this.key = other.key;
        this.type = other.type;
        this.pluginNode = other.pluginNode;
        this.next = other.next != null ? new ArrayList<>(other.next) : null;
        this.config = other.config;
        this.metadata = other.metadata != null ? new HashMap<>(other.metadata) : null;
        this.policy = other.policy != null ? new HashMap<>(other.policy) : null;
    }
}