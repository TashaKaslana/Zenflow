package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.download;

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

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveDownloadExecutor implements NodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String fileId = (String) input.get("fileId");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Drive> handle = driveServiceManager.acquire(refreshToken, resourceConfig)) {
                Drive drive = handle.getResource();

                File metadata = drive.files().get(fileId)
                        .setFields("id, name, mimeType, size")
                        .execute();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                drive.files().get(fileId).executeMediaAndDownloadTo(out);

                String data = Base64.getEncoder().encodeToString(out.toByteArray());

                Map<String, Object> output = new HashMap<>();
                output.put("fileId", metadata.getId());
                output.put("name", metadata.getName());
                output.put("mimeType", metadata.getMimeType());
                output.put("data", data);
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive download failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
