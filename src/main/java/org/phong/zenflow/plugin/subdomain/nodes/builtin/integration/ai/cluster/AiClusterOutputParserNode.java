package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@PluginNode(
        key = "core:ai.output_parser",
        name = "AI Output Parser",
        version = "1.0.0",
        description = "Configure response parsing strategy for AI cluster executions",
        type = "integration.ai",
        tags = {"ai", "parser", "cluster"},
        icon = "mdi:code-json",
        schemaPath = "output-parser.schema.json"
)
public class AiClusterOutputParserNode implements NodeDefinitionProvider {

    private final AiClusterOutputParserExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
