package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.list;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.list",
        name = "Google Drive - List Files",
        version = "1.0.0",
        description = "Lists files from Google Drive using OAuth2 credentials.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "list", "storage"}
)
public class GoogleDriveListNode implements NodeDefinitionProvider {
    private final GoogleDriveListExecutor executor;
    private final GoogleDriveServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
