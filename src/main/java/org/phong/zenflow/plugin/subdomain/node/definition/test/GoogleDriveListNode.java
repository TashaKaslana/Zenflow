package org.phong.zenflow.plugin.subdomain.node.definition.test;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
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
    private final GoogleDriveListExecutor googleDriveListExecutor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(googleDriveListExecutor)
                .name("Google Drive - List Files")
                .description("Lists files from Google Drive using OAuth2 credentials.")
                .icon("googleDrive")
                .type("integration.storage")
                .tags(new String[]{"google", "drive", "list", "storage"})
                .build();
    }
}

