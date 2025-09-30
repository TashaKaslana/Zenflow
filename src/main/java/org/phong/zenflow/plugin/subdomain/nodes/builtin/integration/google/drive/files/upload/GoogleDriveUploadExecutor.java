package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.upload;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveUploadExecutor implements NodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String name = (String) input.get("name");
            String mimeType = (String) input.getOrDefault("mimeType", "application/octet-stream");
            String data = (String) input.get("data");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Drive> handle = driveServiceManager.acquire(refreshToken, resourceConfig)) {
                Drive drive = handle.getResource();

                byte[] fileBytes = Base64.getDecoder().decode(data);
                ByteArrayContent mediaContent = new ByteArrayContent(mimeType, fileBytes);

                File fileMetadata = new File();
                fileMetadata.setName(name);
                fileMetadata.setMimeType(mimeType);

                File file = drive.files().create(fileMetadata, mediaContent)
                        .setFields("id, name, mimeType, size, webViewLink")
                        .execute();

                Map<String, Object> output = new HashMap<>();
                output.put("file", file);
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive upload failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
