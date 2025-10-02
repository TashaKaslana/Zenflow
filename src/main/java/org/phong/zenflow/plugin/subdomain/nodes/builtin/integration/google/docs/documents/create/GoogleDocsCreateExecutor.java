package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.create;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
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
public class GoogleDocsCreateExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) throws IOException {
        Map<String, Object> input = config.input();
        String title = (String) input.getOrDefault("title", "Untitled Document");

        Docs docs = context.getResource(Docs.class);

        Document request = new Document();
        request.setTitle(title);

        Document created = docs.documents().create(request).execute();

        Map<String, Object> output = new HashMap<>();
        output.put("documentId", created.getDocumentId());
        output.put("title", created.getTitle());
        return ExecutionResult.success(output);
    }
}
