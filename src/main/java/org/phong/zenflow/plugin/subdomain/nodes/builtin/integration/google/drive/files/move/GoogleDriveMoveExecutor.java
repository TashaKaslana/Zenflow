package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.move;

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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDriveMoveExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) throws IOException {
        Map<String, Object> input = config.input();
        String fileId = (String) input.get("fileId");
        String destinationFolderId = (String) input.get("destinationFolderId");
        String sourceFolderId = (String) input.get("sourceFolderId");

        Drive drive = context.getResource(Drive.class);

        if (sourceFolderId == null || sourceFolderId.isBlank()) {
            File current = drive.files().get(fileId).setFields("parents").execute();
            if (current.getParents() != null && !current.getParents().isEmpty()) {
                sourceFolderId = String.join(",", current.getParents());
            }
        }

        Drive.Files.Update update = drive.files().update(fileId, null)
                .setAddParents(destinationFolderId);
        if (sourceFolderId != null && !sourceFolderId.isBlank()) {
            update.setRemoveParents(sourceFolderId);
        }

        File moved = update.setFields("id,parents").execute();

        Map<String, Object> output = new HashMap<>();
        output.put("id", moved.getId());
        output.put("parents", moved.getParents());
        return ExecutionResult.success(output);
    }
}
