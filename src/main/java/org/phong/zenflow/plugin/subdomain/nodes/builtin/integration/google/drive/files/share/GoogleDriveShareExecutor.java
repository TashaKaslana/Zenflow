package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.share;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Shares a file by creating a permission on Google Drive.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveShareExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String fileId = context.read("fileId", String.class);
        String role = context.read("role", String.class);
        String type = context.read("type", String.class);
        String emailAddress = context.read("emailAddress", String.class);

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

        context.write("permission", created);
        return ExecutionResult.success();
    }
}
