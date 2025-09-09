package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.share;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Shares a file by creating a permission on Google Drive.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-drive:files.share",
        name = "Google Drive - Share File",
        version = "1.0.0",
        description = "Shares a file with a user, domain, group or anyone.",
        icon = "googleDrive",
        type = "integration.storage",
        tags = {"google", "drive", "share", "storage"}
)
public class GoogleDriveShareExecutor implements PluginNodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String profile = (String) input.get("profile");
            String fileId = (String) input.get("fileId");
            String role = (String) input.get("role");
            String type = (String) input.get("type");
            String emailAddress = (String) input.get("emailAddress");

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

            try (ScopedNodeResource<Drive> handle =
                         driveServiceManager.acquire(refreshToken, context.getWorkflowRunId(), resourceConfig)) {
                Drive drive = handle.getResource();

                Permission permission = new Permission()
                        .setRole(role)
                        .setType(type);
                if (emailAddress != null) {
                    permission.setEmailAddress(emailAddress);
                }

                Permission created = drive.permissions()
                        .create(fileId, permission)
                        .setFields("id, type, role, emailAddress")
                        .execute();

                Map<String, Object> output = new HashMap<>();
                output.put("permission", created);
                return ExecutionResult.success(output);
            }
        } catch (Exception e) {
            logCollector.withException(e).error("Google Drive share failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}

