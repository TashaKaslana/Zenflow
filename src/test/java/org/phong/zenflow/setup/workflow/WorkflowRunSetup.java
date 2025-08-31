package org.phong.zenflow.setup.workflow;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.setup.ContextSetupHolder;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.repository.NodeExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @SuppressWarnings("unchecked")
    public void startWorkflows() {
        Map<String, UUID> workflowRunIds = new HashMap<>();
        Map<String, UUID> workflowIds = (Map<String, UUID>) ContextSetupHolder.get("workflows");

        workflowIds.forEach((key, value) -> {
            workflowService.activateWorkflow(value);
            UUID executed = workflowService.executeWorkflow(value, "start");
            workflowRunIds.put(key, executed);
        });

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
