package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.workflow_trigger;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:workflow.trigger",
        name = "Workflow Trigger",
        version = "1.0.0",
        type = "trigger",
        triggerType = "manual",
        description = "Triggers a workflow execution based on provided parameters.",
        tags = {"workflow", "trigger", "execution"},
        icon = "ph:rocket-launch"
)
public class WorkflowTriggerNode implements NodeDefinitionProvider {
    private final WorkflowTriggerExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
