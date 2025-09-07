package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.download;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.share.GoogleDriveServiceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-drive:files.download",
        name = "Google Drive - Download File",
        version = "1.0.0",
        description = "Downloads a file from Google Drive and returns its content encoded in Base64.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "download", "storage"}
)
public class GoogleDriveDownloadExecutor implements PluginNodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String profile = (String) input.get("profile");
            String fileId = (String) input.get("fileId");

            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> secretMap = context.read("secrets", Map.class);
            Map<String, String> credentials = secretMap.get(profile);
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
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive download failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}

