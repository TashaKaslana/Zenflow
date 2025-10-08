package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.upload;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveUploadExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) throws IOException {
        Map<String, Object> input = config.input();
        String name = (String) input.get("name");
        String mimeType = (String) input.getOrDefault("mimeType", "application/octet-stream");
        String data = (String) input.get("data");

        Drive drive = context.getResource(Drive.class);

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
}
