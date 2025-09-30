package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.add_header;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-docs:documents.add_header",
        name = "Google Docs - Add Header",
        version = "1.0.0",
        description = "Creates a header in a Google Docs document and inserts provided text.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "header"}
)
public class GoogleDocsAddHeaderNode implements NodeDefinitionProvider {
    private final GoogleDocsAddHeaderExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
