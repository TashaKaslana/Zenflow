package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.share;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Shares a file by creating a permission on Google Drive.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveShareExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) throws IOException {
        Map<String, Object> input = config.input();
        String fileId = (String) input.get("fileId");
        String role = (String) input.get("role");
        String type = (String) input.get("type");
        String emailAddress = (String) input.get("emailAddress");

        Drive drive = context.getResource(Drive.class);

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
}
