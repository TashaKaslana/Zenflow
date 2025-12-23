package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeState;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeStateType;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.executor.HttpRequestExecutor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@AllArgsConstructor
@PluginNode(
        key = "core:http.request",
        name = "HTTP Request",
        version = "1.0.0",
        description = "Executes an HTTP request using the specified method and URL, with optional headers and body.",
        tags = {"http", "request", "network"},
        type = "util",
        icon = "ph:globe",
        schemaPath = "schema.json",
        docPath = "doc.md"
)
public class HttpRequestNode implements NodeDefinitionProvider {
    private final HttpRequestExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeState(new NodeState(Set.of(NodeStateType.NORMAL, NodeStateType.TOOL)))
                .build();
    }
}
