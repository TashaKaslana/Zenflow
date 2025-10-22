package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.update;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String fileId = context.read("fileId", String.class);
        String name = context.read("name", String.class);
        String description = context.read("description", String.class);
        String mimeType = context.read("mimeType", String.class);
        String contentBase64 = context.read("contentBase64", String.class);

        Drive drive = context.getResource(Drive.class);

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
}
