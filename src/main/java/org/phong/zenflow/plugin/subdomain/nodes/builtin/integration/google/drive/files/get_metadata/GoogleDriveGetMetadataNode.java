package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.get_metadata;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.getMetadata",
        name = "Google Drive - Get File Metadata",
        version = "1.0.0",
        description = "Retrieves metadata for a file in Google Drive using OAuth2 credentials.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "metadata", "storage"}
)
public class GoogleDriveGetMetadataNode implements NodeDefinitionProvider {
    private final GoogleDriveGetMetadataExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
