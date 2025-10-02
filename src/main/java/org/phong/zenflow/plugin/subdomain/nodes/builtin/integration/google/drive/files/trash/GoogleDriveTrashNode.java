package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.trash;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.trash",
        name = "Google Drive - Trash File",
        version = "1.0.0",
        description = "Moves a file to the trash in Google Drive.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "trash", "storage"}
)
public class GoogleDriveTrashNode implements NodeDefinitionProvider {
    private final GoogleDriveTrashExecutor executor;
    private final GoogleDriveServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
