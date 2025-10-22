package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.copy;

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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveCopyExecutor implements NodeExecutor {


    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String fileId = context.read("fileId", String.class);
        String destinationFolderId = context.read("destinationFolderId", String.class);
        String name = context.read("name", String.class);

        Drive drive = context.getResource();

        File copyMeta = new File();
        if (name != null) {
            copyMeta.setName(name);
        }
        if (destinationFolderId != null) {
            copyMeta.setParents(List.of(destinationFolderId));
        }

        File copied = drive.files().copy(fileId, copyMeta)
                .setFields("id,name,mimeType,size,parents,webViewLink")
                .execute();

        Map<String, Object> output = new HashMap<>();
        output.put("id", copied.getId());
        output.put("name", copied.getName());
        output.put("mimeType", copied.getMimeType());
        output.put("size", copied.getSize());
        output.put("parents", copied.getParents());
        output.put("webViewLink", copied.getWebViewLink());
        return ExecutionResult.success(output);
    }
}

