package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.move;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.move",
        name = "Google Drive - Move File",
        version = "1.0.0",
        description = "Moves a file to a different folder in Google Drive.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "move", "storage"}
)
public class GoogleDriveMoveNode implements NodeDefinitionProvider {
    private final GoogleDriveMoveExecutor executor;
    private final GoogleDriveServiceManager resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
