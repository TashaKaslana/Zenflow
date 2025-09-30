package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.add_comment;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-docs:documents.add_comment",
        name = "Google Docs - Add Comment",
        version = "1.0.0",
        description = "Adds a comment to a Google Docs document using the Drive API.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "comment"}
)
public class GoogleDocsAddCommentNode implements NodeDefinitionProvider {
    private final GoogleDocsAddCommentExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
