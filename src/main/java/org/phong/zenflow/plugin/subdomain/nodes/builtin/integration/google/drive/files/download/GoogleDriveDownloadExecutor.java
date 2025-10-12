package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.download;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveDownloadExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String fileId = context.read("fileId", String.class);

        Drive drive = context.getResource(Drive.class);

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
}
