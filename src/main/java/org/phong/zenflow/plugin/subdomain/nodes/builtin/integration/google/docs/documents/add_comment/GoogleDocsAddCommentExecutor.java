package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.add_comment;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Comment;
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
public class GoogleDocsAddCommentExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) throws IOException  {
        String documentId = context.read("documentId", String.class);
        String content = context.read("content", String.class);

        Drive drive = context.getResource(Drive.class);

        Comment comment = new Comment();
        comment.setContent(content);
        Comment created = drive.comments().create(documentId, comment).execute();

        context.write("documentId", documentId);
        context.write("commentId", created.getId());
        return ExecutionResult.success();
    }
}
