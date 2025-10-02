package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.add_comment;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Comment;
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
public class GoogleDocsAddCommentExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) throws IOException {
        Map<String, Object> input = config.input();
        String documentId = (String) input.get("documentId");
        String content = (String) input.get("content");

        Drive drive = context.getResource(Drive.class);

        Comment comment = new Comment();
        comment.setContent(content);
        Comment created = drive.comments().create(documentId, comment).execute();

        Map<String, Object> output = new HashMap<>();
        output.put("documentId", documentId);
        output.put("commentId", created.getId());
        return ExecutionResult.success(output);
    }
}
