package org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public final class PluginDefinition extends BaseWorkflowNode {
    @NotNull
    private final PluginNodeDefinition pluginNode;

    public PluginDefinition(String key, NodeType type, List<String> next, Map<String, Object> config,
                            Map<String, Object> metadata, Map<String, Object> policy, PluginNodeDefinition pluginNode) {
        super(key, type, next, config, metadata, policy);
        this.pluginNode = pluginNode;
    }
}
