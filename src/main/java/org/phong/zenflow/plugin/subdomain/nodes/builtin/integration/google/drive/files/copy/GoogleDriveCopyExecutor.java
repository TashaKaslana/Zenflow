package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.copy;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleCredentialsException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleResourceConfigBuilder;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-drive:files.copy",
        name = "Google Drive - Copy File",
        version = "1.0.0",
        description = "Creates a copy of a file in Google Drive.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "copy", "storage"}
)
public class GoogleDriveCopyExecutor implements PluginNodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String fileId = (String) input.get("fileId");
            String destinationFolderId = (String) input.get("destinationFolderId");
            String name = (String) input.get("name");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Drive> handle = driveServiceManager.acquire(refreshToken, resourceConfig)) {
                Drive drive = handle.getResource();

                File copyMeta = new File();
                if (name != null) {
                    copyMeta.setName(name);
                }
                if (destinationFolderId != null) {
                    copyMeta.setParents(List.of(destinationFolderId));
                }

                File copied = drive.files().copy(fileId, copyMeta)
                        .setFields("id,name,mimeType,size,parents,webViewLink")
                        .execute();

                Map<String, Object> output = new HashMap<>();
                output.put("id", copied.getId());
                output.put("name", copied.getName());
                output.put("mimeType", copied.getMimeType());
                output.put("size", copied.getSize());
                output.put("parents", copied.getParents());
                output.put("webViewLink", copied.getWebViewLink());
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive copy failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
