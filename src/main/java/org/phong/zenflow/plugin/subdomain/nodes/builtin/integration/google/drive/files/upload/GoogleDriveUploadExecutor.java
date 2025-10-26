package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.upload;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveUploadExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String name = context.read("name", String.class);
        String mimeType = context.read("mimeType", String.class);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        String data = context.read("data", String.class);

        Drive drive = context.getResource(Drive.class);

        byte[] fileBytes = Base64.getDecoder().decode(data);
        ByteArrayContent mediaContent = new ByteArrayContent(mimeType, fileBytes);

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType(mimeType);

        File file = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, mimeType, size, webViewLink")
                .execute();

        context.write("file", file);
        return ExecutionResult.success();
    }
}
