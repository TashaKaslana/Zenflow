package org.phong.zenflow.plugin.subdomain.nodes.remote;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "remote:node",
        name = "Remote Node",
        version = "1.0.0",
        description = "Executes a remote node by sending a request to a configured endpoint.",
        type = "remote",
        tags = {"remote", "external", "api"},
        icon = "ph:cloud-arrow-up"
)
public class RemoteNode implements NodeDefinitionProvider {
    private final RemoteNodeExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
