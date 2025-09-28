package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.format_text;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Range;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.TextStyle;
import com.google.api.services.docs.v1.model.UpdateTextStyleRequest;
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
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@PluginNode(
        key = "google-docs:documents.format_text",
        name = "Google Docs - Format Text",
        version = "1.0.0",
        description = "Applies text styling, such as bold or font size, to a range within a Google Docs document.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "format", "text"}
)
public class GoogleDocsFormatTextExecutor implements PluginNodeExecutor {

    private final GoogleDocsServiceManager docsServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String documentId = (String) input.get("documentId");
            Number start = (Number) input.get("startIndex");
            Number end = (Number) input.get("endIndex");
            Boolean bold = (Boolean) input.getOrDefault("bold", Boolean.FALSE);
            Number fontSize = (Number) input.get("fontSize");

            DefaultResourceConfig resourceConfig = GoogleResourceConfigBuilder.build(context);
            String refreshToken = resourceConfig.getResourceIdentifier();

            try (ScopedNodeResource<Docs> handle =
                         docsServiceManager.acquire(refreshToken, resourceConfig)) {
                Docs docs = handle.getResource();

                TextStyle style = new TextStyle().setBold(bold);
                if (fontSize != null) {
                    style.setFontSize(new com.google.api.services.docs.v1.model.Dimension()
                            .setMagnitude(fontSize.doubleValue())
                            .setUnit("PT"));
                }

                UpdateTextStyleRequest styleRequest = new UpdateTextStyleRequest()
                        .setRange(new Range().setStartIndex(start.intValue()).setEndIndex(end.intValue()))
                        .setTextStyle(style)
                        .setFields(fontSize != null ? "bold,fontSize" : "bold");

                Request request = new Request().setUpdateTextStyle(styleRequest);
                BatchUpdateDocumentRequest body = new BatchUpdateDocumentRequest().setRequests(List.of(request));
                docs.documents().batchUpdate(documentId, body).execute();

                Map<String, Object> output = new HashMap<>();
                output.put("documentId", documentId);
                output.put("formatted", true);
                return ExecutionResult.success(output);
            }
        } catch (GoogleCredentialsException e) {
            logCollector.withException(e).error("Google credential error: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        } catch (Exception e) {
            logCollector.withException(e).error("Google Docs format text failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
