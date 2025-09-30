package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.share;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.share",
        name = "Google Drive - Share File",
        version = "1.0.0",
        description = "Shares a file with a user, domain, group or anyone.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "share", "storage"}
)
public class GoogleDriveShareNode implements NodeDefinitionProvider {
    private final GoogleDriveShareExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
