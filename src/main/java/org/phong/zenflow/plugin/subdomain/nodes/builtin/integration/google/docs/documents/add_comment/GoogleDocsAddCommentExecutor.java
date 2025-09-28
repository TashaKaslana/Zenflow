package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.add_comment;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleCredentialsException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleResourceConfigBuilder;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.drive.GoogleDriveServiceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-docs:documents.add_comment",
        name = "Google Docs - Add Comment",
        version = "1.0.0",
        description = "Adds a comment to a Google Docs document using the Drive API.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "comment"}
)
public class GoogleDocsAddCommentExecutor implements PluginNodeExecutor {

    private final GoogleDriveServiceManager driveServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String documentId = (String) input.get("documentId");
            String content = (String) input.get("content");

            DefaultTriggerResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Drive> handle =
                         driveServiceManager.acquire(refreshToken, context.getWorkflowRunId(), resourceConfig)) {
                Drive drive = handle.getResource();

                Comment comment = new Comment();
                comment.setContent(content);
                Comment created = drive.comments().create(documentId, comment).execute();

                Map<String, Object> output = new HashMap<>();
                output.put("documentId", documentId);
                output.put("commentId", created.getId());
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Docs add comment failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
