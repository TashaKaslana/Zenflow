package org.phong.zenflow.setup.workflow;

import org.phong.zenflow.setup.ContextSetupHolder;
import org.phong.zenflow.setup.TestDataConfiguration;
import org.phong.zenflow.workflow.dto.CreateWorkflowRequest;
import org.phong.zenflow.workflow.dto.WorkflowDto;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class WorkflowSetup {
    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private TestDataConfiguration testDataConfig;

    @SuppressWarnings("unchecked")
    public void setupWorkflows() {
        Map<String, UUID> projectMap = (Map<String, UUID>) ContextSetupHolder.get("projects");

        if (projectMap == null || projectMap.isEmpty()) {
            throw new IllegalStateException("Projects must be set up before workflows");
        }

        // Use configuration data for workflow descriptions
        List<CreateWorkflowRequest> workflows = testDataConfig.getWorkflows().getDescriptions().entrySet().stream()
                .map(entry -> {
                    String workflowName = entry.getKey();
                    String description = entry.getValue();
                    // Assign workflows to projects based on naming convention
                    UUID projectId = "standard-workflow".equals(workflowName)
                            ? projectMap.get("alpha")
                            : projectMap.get("beta");
                    return new CreateWorkflowRequest(
                            projectId,
                            workflowName,
                            description,
                            new WorkflowDefinition(),
                            null
                    );
                })
                .toList();

        List<WorkflowDto> workflowDtos = workflowService.createWorkflows(workflows);
        Map<String, UUID> workflowIdMap = workflowDtos.stream()
                .collect(java.util.stream.Collectors.toMap(
                        WorkflowDto::name,
                        WorkflowDto::id
                ));

        ContextSetupHolder.set("workflows", workflowIdMap);
    }
}
