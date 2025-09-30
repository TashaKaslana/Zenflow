package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.add_header;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.CreateHeaderRequest;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Location;
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
public class GoogleDocsAddHeaderExecutor implements NodeExecutor {

    private final GoogleDocsServiceManager docsServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String documentId = (String) input.get("documentId");
            String text = (String) input.getOrDefault("text", "");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Docs> handle =
                         docsServiceManager.acquire(refreshToken, resourceConfig)) {
                Docs docs = handle.getResource();

                Request createHeader = new Request().setCreateHeader(new CreateHeaderRequest());
                BatchUpdateDocumentResponse response = docs.documents().batchUpdate(documentId,
                        new BatchUpdateDocumentRequest().setRequests(List.of(createHeader))).execute();
                String headerId = response.getReplies().getFirst().getCreateHeader().getHeaderId();

                if (text != null && !text.isEmpty()) {
                    InsertTextRequest insert = new InsertTextRequest()
                            .setText(text)
                            .setLocation(new Location().setSegmentId(headerId).setIndex(0));
                    docs.documents().batchUpdate(documentId,
                            new BatchUpdateDocumentRequest().setRequests(List.of(new Request().setInsertText(insert))))
                            .execute();
                }

                Map<String, Object> output = new HashMap<>();
                output.put("documentId", documentId);
                output.put("headerId", headerId);
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Docs add header failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
