package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.update;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.update",
        name = "Google Drive - Update File",
        version = "1.0.0",
        description = "Updates file metadata or content in Google Drive.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "update", "storage"}
)
public class GoogleDriveUpdateNode implements NodeDefinitionProvider {
    private final GoogleDriveUpdateExecutor executor;
    private final GoogleDriveServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
