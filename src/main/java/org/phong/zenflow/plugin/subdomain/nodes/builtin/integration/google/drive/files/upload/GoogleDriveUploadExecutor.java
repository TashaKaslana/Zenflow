package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.upload;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-drive:files.upload",
        name = "Google Drive - Upload File",
        version = "1.0.0",
        description = "Uploads a file to Google Drive from a Base64 encoded content.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "upload", "storage"}
)
public class GoogleDriveUploadExecutor implements PluginNodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String profile = (String) input.get("profile");
            String name = (String) input.get("name");
            String mimeType = (String) input.getOrDefault("mimeType", "application/octet-stream");
            String data = (String) input.get("data");

            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> profileMap = context.read("profiles", Map.class);
            Map<String, String> credentials = profileMap.get(profile);
            String clientId = credentials.get("CLIENT_ID");
            String clientSecret = credentials.get("CLIENT_SECRET");
            String refreshToken = credentials.get("REFRESH_TOKEN");

            Map<String, Object> cfg = new HashMap<>();
            cfg.put("clientId", clientId);
            cfg.put("clientSecret", clientSecret);
            cfg.put("refreshToken", refreshToken);
            DefaultTriggerResourceConfig resourceConfig = new DefaultTriggerResourceConfig(cfg, "refreshToken");

            try (ScopedNodeResource<Drive> handle = driveServiceManager.acquire(refreshToken, context.getWorkflowRunId(), resourceConfig)) {
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
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive upload failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}

