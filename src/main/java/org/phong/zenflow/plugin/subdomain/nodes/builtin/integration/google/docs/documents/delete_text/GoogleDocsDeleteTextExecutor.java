package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.delete_text;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.DeleteContentRangeRequest;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleCredentialsException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleResourceConfigBuilder;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.GoogleDocsServiceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultTriggerResourceConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-docs:documents.delete_text",
        name = "Google Docs - Delete Text",
        version = "1.0.0",
        description = "Deletes a range of text within a Google Docs document using OAuth2 credentials.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "delete", "text"}
)
public class GoogleDocsDeleteTextExecutor implements PluginNodeExecutor {

    private final GoogleDocsServiceManager docsServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String documentId = (String) input.get("documentId");
            Number start = (Number) input.get("startIndex");
            Number end = (Number) input.get("endIndex");

            DefaultTriggerResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Docs> handle =
                         docsServiceManager.acquire(refreshToken, context.getWorkflowRunId(), resourceConfig)) {
                Docs docs = handle.getResource();

                Range range = new Range().setStartIndex(start.intValue()).setEndIndex(end.intValue());
                Request request = new Request().setDeleteContentRange(
                        new DeleteContentRangeRequest().setRange(range));
                BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(List.of(request));
                docs.documents().batchUpdate(documentId, body).execute();

                Map<String, Object> output = new HashMap<>();
                output.put("documentId", documentId);
                output.put("deleted", true);
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Docs delete text failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
