package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.delete;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.delete",
        name = "Google Drive - Delete File",
        version = "1.0.0",
        description = "Permanently deletes a file from Google Drive.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "delete", "storage"}
)
public class GoogleDriveDeleteNode implements NodeDefinitionProvider {
    private final GoogleDriveDeleteExecutor executor;
    private final GoogleDriveServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
