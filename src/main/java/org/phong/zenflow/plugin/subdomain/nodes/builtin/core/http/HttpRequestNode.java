package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.http.executor.HttpRequestExecutor;
import org.springframework.stereotype.Component;

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
        schemaPath = "../schema.json",
        docPath = "../doc.md"
)
public class HttpRequestNode implements NodeDefinitionProvider {
    private final HttpRequestExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
