package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.files.list;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
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
public class GoogleDriveListExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String query = context.read("query", String.class);
        if (query == null) {
            query = "trashed=false";
        }

        Drive drive = context.getResource(Drive.class);

        FileList result = drive.files().list()
                .setQ(query)
                .setPageSize(10)
                .setFields("files(id, name)")
                .execute();

        context.write("files", result.getFiles());
        return ExecutionResult.success();
    }
}
