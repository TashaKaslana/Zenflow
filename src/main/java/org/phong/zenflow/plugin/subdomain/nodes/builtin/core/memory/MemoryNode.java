package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.memory;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.definition.policy.ContextAccessPolicy;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
    key = "core:context_variable",
    name = "Context Variable",
    version = "1.0.0",
    description = "Store and retrieve variables in workflow context (for AI conversation history, temporary data, etc.)",
    type = "data",
    tags = {"context", "variable", "memory", "storage"},
    icon = "ph:database",
    schemaPath = "schema.json"
)
public class MemoryNode implements NodeDefinitionProvider {
    private final MemoryExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .contextAccessPolicy(ContextAccessPolicy.PERSIST_OUTPUTS)
                .build();
    }
}
