package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.create;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-docs:documents.create",
        name = "Google Docs - Create Document",
        version = "1.0.0",
        description = "Creates a new Google Docs document using OAuth2 credentials.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "create", "document"}
)
public class GoogleDocsCreateNode implements NodeDefinitionProvider {
    private final GoogleDocsCreateExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
