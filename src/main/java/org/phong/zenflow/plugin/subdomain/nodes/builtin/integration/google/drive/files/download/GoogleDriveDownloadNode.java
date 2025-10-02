package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.download;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.download",
        name = "Google Drive - Download File",
        version = "1.0.0",
        description = "Downloads a file from Google Drive and returns its content encoded in Base64.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "download", "storage"}
)
public class GoogleDriveDownloadNode implements NodeDefinitionProvider {
    private final GoogleDriveDownloadExecutor executor;
    private final GoogleDriveServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
