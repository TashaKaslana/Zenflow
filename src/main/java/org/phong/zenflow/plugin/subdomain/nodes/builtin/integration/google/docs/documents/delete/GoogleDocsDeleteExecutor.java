package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.delete;

import com.google.api.services.drive.Drive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDocsDeleteExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String documentId = context.read("documentId", String.class);

        Drive drive = context.getResource(Drive.class);
        drive.files().delete(documentId).execute();

        context.write("documentId", documentId);
        context.write("deleted", true);
        return ExecutionResult.success();
    }
}