package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.update;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleCredentialsException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleResourceConfigBuilder;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Updates metadata or content of a file in Google Drive.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveUpdateExecutor implements NodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String fileId = (String) input.get("fileId");
            String name = (String) input.get("name");
            String description = (String) input.get("description");
            String mimeType = (String) input.get("mimeType");
            String contentBase64 = (String) input.get("contentBase64");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Drive> handle =
                         driveServiceManager.acquire(refreshToken, resourceConfig)) {
                Drive drive = handle.getResource();

                File fileMetadata = new File();
                if (name != null) fileMetadata.setName(name);
                if (description != null) fileMetadata.setDescription(description);
                if (mimeType != null) fileMetadata.setMimeType(mimeType);

                Drive.Files.Update update;
                if (contentBase64 != null) {
                    byte[] bytes = Base64.getDecoder().decode(contentBase64);
                    ByteArrayContent media = new ByteArrayContent(
                            mimeType != null ? mimeType : "application/octet-stream", bytes);
                    update = drive.files().update(fileId, fileMetadata, media);
                } else {
                    update = drive.files().update(fileId, fileMetadata);
                }

                File updated = update
                        .setFields("id, name, mimeType, description, size, modifiedTime")
                        .execute();

                Map<String, Object> output = new HashMap<>();
                output.put("file", updated);
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive update failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
