package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.list;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.share.GoogleDriveServiceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-drive:files.list",
        name = "Google Drive - List Files",
        version = "1.0.0",
        description = "Lists files from Google Drive using OAuth2 credentials.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "list", "storage"}
)
public class GoogleDriveListExecutor implements PluginNodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String profile = (String) input.get("profile");
            String query = (String) input.getOrDefault("query", "trashed=false");

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

                FileList result = drive.files().list()
                        .setQ(query)
                        .setPageSize(10)
                        .setFields("files(id, name)")
                        .execute();

                Map<String, Object> output = new HashMap<>();
                output.put("files", result.getFiles());
                return ExecutionResult.success(output);
            }
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive list failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
