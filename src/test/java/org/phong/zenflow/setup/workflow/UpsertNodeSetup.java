package org.phong.zenflow.setup.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.phong.zenflow.setup.ContextSetupHolder;
import org.phong.zenflow.workflow.dto.WorkflowDefinitionChangeRequest;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class UpsertNodeSetup {
    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public void setupWorkflowDefinitions() {
        Map<String, UUID> o = (Map<String, UUID>) ContextSetupHolder.get("workflows");
        UUID workflowId = o.get("standard-workflow");
        AtomicInteger i = new AtomicInteger(0);

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:test-data/*.json");

            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    WorkflowDefinitionChangeRequest request = objectMapper.readValue(inputStream, WorkflowDefinitionChangeRequest.class);
                    workflowService.updateWorkflowDefinition(workflowId, request);
                    i.getAndIncrement();
                } catch (IOException e) {
                    // Log a warning instead of throwing an exception
                    System.err.println("Failed to process workflow definition from " + resource.getFilename() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // Log a warning if the resource path is invalid
            System.err.println("Failed to scan for workflow definition files: " + e.getMessage());
        }
        ContextSetupHolder.set("upsertedNodeCount", i.get());
    }
}