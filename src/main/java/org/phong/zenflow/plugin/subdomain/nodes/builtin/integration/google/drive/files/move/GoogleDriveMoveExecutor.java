package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.move;

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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-drive:files.move",
        name = "Google Drive - Move File",
        version = "1.0.0",
        description = "Moves a file to a different folder in Google Drive.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "move", "storage"}
)
public class GoogleDriveMoveExecutor implements PluginNodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String fileId = (String) input.get("fileId");
            String destinationFolderId = (String) input.get("destinationFolderId");
            String sourceFolderId = (String) input.get("sourceFolderId");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Drive> handle = driveServiceManager.acquire(refreshToken, resourceConfig)) {
                Drive drive = handle.getResource();

                if (sourceFolderId == null || sourceFolderId.isBlank()) {
                    File current = drive.files().get(fileId).setFields("parents").execute();
                    if (current.getParents() != null && !current.getParents().isEmpty()) {
                        sourceFolderId = String.join(",", current.getParents());
                    }
                }

                Drive.Files.Update update = drive.files().update(fileId, null)
                        .setAddParents(destinationFolderId);
                if (sourceFolderId != null && !sourceFolderId.isBlank()) {
                    update.setRemoveParents(sourceFolderId);
                }

                File moved = update.setFields("id,parents").execute();

                Map<String, Object> output = new HashMap<>();
                output.put("id", moved.getId());
                output.put("parents", moved.getParents());
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive move failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
