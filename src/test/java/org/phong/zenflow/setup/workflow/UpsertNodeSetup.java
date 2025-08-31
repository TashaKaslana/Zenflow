package org.phong.zenflow.setup.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.phong.zenflow.setup.ContextSetupHolder;
import org.phong.zenflow.workflow.dto.WorkflowDefinitionChangeRequest;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Component
public class UpsertNodeSetup {
    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public void setupWorkflowDefinitions() {
        Map<String, UUID> o = (Map<String, UUID>) ContextSetupHolder.get("workflows");

        workflowService.updateWorkflowDefinition(o.get("standard-workflow"), firstWorkflowDefinition());
    }

    private WorkflowDefinitionChangeRequest firstWorkflowDefinition() {
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("test-data/standard-workflow-definitions-test.json");

            if (inputStream == null) {
                throw new RuntimeException("Could not find standard-workflow-definitions-test.json in test resources");
            }

            // Directly deserialize JSON into WorkflowDefinitionChangeRequest - simpler and more realistic
            return objectMapper.readValue(inputStream, WorkflowDefinitionChangeRequest.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read workflow definition from JSON file", e);
        }
    }
}
