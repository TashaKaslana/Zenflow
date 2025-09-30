package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.append_text;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.EndOfSegmentLocation;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleCredentialsException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.core.GoogleResourceConfigBuilder;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.GoogleDocsServiceManager;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleDocsAppendTextExecutor implements NodeExecutor {

    private final GoogleDocsServiceManager docsServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String documentId = (String) input.get("documentId");
            String text = (String) input.get("text");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Docs> handle =
                         docsServiceManager.acquire(refreshToken, resourceConfig)) {
                Docs docs = handle.getResource();

                InsertTextRequest insert = new InsertTextRequest()
                        .setText(text)
                        .setEndOfSegmentLocation(new EndOfSegmentLocation());
                Request request = new Request().setInsertText(insert);
                BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest()
                        .setRequests(List.of(request));

                docs.documents().batchUpdate(documentId, body).execute();

                Map<String, Object> output = new HashMap<>();
                output.put("documentId", documentId);
                output.put("text", text);
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Docs append text failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
