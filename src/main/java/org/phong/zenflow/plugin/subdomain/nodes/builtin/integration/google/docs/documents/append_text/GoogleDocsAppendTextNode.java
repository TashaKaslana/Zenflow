package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.append_text;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-docs:documents.append_text",
        name = "Google Docs - Append Text",
        version = "1.0.0",
        description = "Appends text to the end of a Google Docs document using OAuth2 credentials.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "append", "text", "document"}
)
public class GoogleDocsAppendTextNode implements NodeDefinitionProvider {
    private final GoogleDocsAppendTextExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
