package org.phong.zenflow.workflow.subdomain.node_definition.definitions.trigger;

import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TriggerNodeDefinition extends BaseWorkflowNode implements Serializable {
    private final TriggerType triggerType;

    public TriggerNodeDefinition(String key, NodeType type, List<String> next, WorkflowConfig config, Map<String, Object> metadata, Map<String, Object> policy, TriggerType triggerType, PluginNodeIdentifier pluginNode) {
        super(key, type, next, config, metadata, policy, pluginNode);
        this.triggerType = triggerType;
    }
}
