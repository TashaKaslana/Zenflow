package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.get_metadata;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveGetMetadataExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String fileId = context.read("fileId", String.class);

        Drive drive = context.getResource(Drive.class);
        File file = drive.files().get(fileId)
                .setFields("id, name, mimeType, size, modifiedTime, trashed, parents, webViewLink")
                .execute();

        Map<String, Object> output = new HashMap<>();
        output.put("file", file);
        return ExecutionResult.success(output);
    }
}
