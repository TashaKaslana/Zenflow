package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.copy;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "google-drive:files.copy",
        name = "Google Drive - Copy File",
        version = "1.0.0",
        description = "Creates a copy of a file in Google Drive.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "copy", "storage"}
)
public class GoogleDriveCopyNode implements NodeDefinitionProvider {
    private final GoogleDriveCopyExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
