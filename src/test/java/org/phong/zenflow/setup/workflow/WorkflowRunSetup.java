package org.phong.zenflow.setup.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.setup.ContextSetupHolder;
import org.phong.zenflow.workflow.dto.WorkflowDefinitionChangeRequest;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.repository.NodeExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
@Slf4j
public class WorkflowRunSetup {
    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private NodeExecutionRepository nodeExecutionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public void startWorkflows() {
        Map<String, UUID> workflowRunIds = new HashMap<>();
        Map<String, UUID> workflowIds = (Map<String, UUID>) ContextSetupHolder.get("workflows");
        UUID standardWorkflowId = workflowIds.get("standard-workflow");

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:test-data/*.json");

            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    WorkflowDefinitionChangeRequest request = objectMapper.readValue(inputStream, WorkflowDefinitionChangeRequest.class);
                    workflowService.updateWorkflowDefinition(standardWorkflowId, request);
                    workflowService.activateWorkflow(standardWorkflowId);
                    UUID executed = workflowService.executeWorkflow(standardWorkflowId, "start");
                    workflowRunIds.put(resource.getFilename(), executed);
                } catch (IOException e) {
                    log.error("Failed to process and run workflow for resource: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan for workflow definition files.", e);
        }

        ContextSetupHolder.set("workflowRuns", workflowRunIds);
        assertTrue(isAllWorkflowsSuccessful(workflowRunIds));
    }

    public boolean isAllWorkflowsSuccessful(Map<String, UUID> workflowRunIds) {
        Map<UUID, String> reverseMap = new HashMap<>();
        workflowRunIds.forEach((key, value) -> reverseMap.put(value, key));

        for (var nodeExecution : nodeExecutionRepository.findAllById(workflowRunIds.values())) {
            if (nodeExecution.getStatus().isFailure()) {
                log.error("Workflow run with ID [{}] and key [{}] did not complete successfully. Status: [{}]",
                        nodeExecution.getId(),
                        reverseMap.getOrDefault(nodeExecution.getId(), "unknown"),
                        nodeExecution.getStatus());
                return false;
            }
        }

        return true;
    }
}