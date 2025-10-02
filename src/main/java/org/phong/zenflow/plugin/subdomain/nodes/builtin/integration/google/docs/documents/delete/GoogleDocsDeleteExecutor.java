package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.delete;

import com.google.api.services.drive.Drive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDocsDeleteExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) throws IOException {
        Map<String, Object> input = config.input();
        String documentId = (String) input.get("documentId");

        Drive drive = context.getResource(Drive.class);
        drive.files().delete(documentId).execute();

        Map<String, Object> output = new HashMap<>();
        output.put("documentId", documentId);
        output.put("deleted", true);
        return ExecutionResult.success(output);
    }
}
