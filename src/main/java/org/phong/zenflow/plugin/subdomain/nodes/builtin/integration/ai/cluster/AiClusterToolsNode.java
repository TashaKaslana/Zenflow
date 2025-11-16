package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@PluginNode(
        key = "core:ai.cluster.tools",
        name = "AI Cluster Tools",
        version = "1.0.0",
        description = "Configure tool packs and custom tool wiring for AI cluster executions",
        type = "integration.ai",
        tags = {"ai", "tools", "cluster"},
        icon = "mdi:tools",
        schemaPath = "tools.schema.json"
)
public class AiClusterToolsNode implements NodeDefinitionProvider {

    private final AiClusterToolsExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
