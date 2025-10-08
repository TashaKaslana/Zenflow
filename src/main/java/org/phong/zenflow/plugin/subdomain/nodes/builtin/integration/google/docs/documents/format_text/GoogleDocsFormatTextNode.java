package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.format_text;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.GoogleDocsServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-docs:documents.format_text",
        name = "Google Docs - Format Text",
        version = "1.0.0",
        description = "Applies text styling, such as bold or font size, to a range within a Google Docs document.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "format", "text"}
)
public class GoogleDocsFormatTextNode implements NodeDefinitionProvider {
    private final GoogleDocsFormatTextExecutor executor;
    private final GoogleDocsServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
