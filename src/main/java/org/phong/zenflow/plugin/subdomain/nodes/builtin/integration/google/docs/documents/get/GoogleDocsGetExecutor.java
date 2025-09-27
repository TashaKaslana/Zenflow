package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.documents.get;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.docs.GoogleDocsServiceManager;
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
        key = "google-docs:documents.get",
        name = "Google Docs - Get Document",
        version = "1.0.0",
        description = "Retrieves a Google Docs document using OAuth2 credentials.",
        icon = "simple-icons:googledocs",
        type = "integration.documents",
        tags = {"google", "docs", "get", "document"}
)
public class GoogleDocsGetExecutor implements PluginNodeExecutor {

    private final GoogleDocsServiceManager docsServiceManager;

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        try {
            Map<String, Object> input = config.input();
            String documentId = (String) input.get("documentId");

            // Get profile credentials using the proper getProfileSecret method
            String clientId = (String) context.getProfileSecret("CLIENT_ID");
            String clientSecret = (String) context.getProfileSecret("CLIENT_SECRET");
            String refreshToken = (String) context.getProfileSecret("CLIENT_REFRESH_TOKEN");

            if (clientId == null || clientSecret == null || refreshToken == null) {
                return ExecutionResult.error("No valid Google OAuth profile found. Please ensure a Google profile is properly configured and linked to this node.");
            }

            Map<String, Object> cfg = new HashMap<>();
            cfg.put("clientId", clientId);
            cfg.put("clientSecret", clientSecret);
            cfg.put("refreshToken", refreshToken);
            DefaultTriggerResourceConfig resourceConfig = new DefaultTriggerResourceConfig(cfg, "refreshToken");

            try (ScopedNodeResource<Docs> handle =
                         docsServiceManager.acquire(refreshToken, context.getWorkflowRunId(), resourceConfig)) {
                Docs docs = handle.getResource();

                Document document = docs.documents().get(documentId).execute();

                Map<String, Object> output = new HashMap<>();
                output.put("documentId", document.getDocumentId());
                output.put("title", document.getTitle());
                output.put("document", document);
                return ExecutionResult.success(output);
            }
        } catch (Exception e) {
            logCollector.withException(e).error("Google Docs get failed: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }
}
