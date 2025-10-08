package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.upload;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.upload",
        name = "Google Drive - Upload File",
        version = "1.0.0",
        description = "Uploads a file to Google Drive from a Base64 encoded content.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "upload", "storage"}
)
public class GoogleDriveUploadNode implements NodeDefinitionProvider {
    private final GoogleDriveUploadExecutor executor;
    private final GoogleDriveServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
