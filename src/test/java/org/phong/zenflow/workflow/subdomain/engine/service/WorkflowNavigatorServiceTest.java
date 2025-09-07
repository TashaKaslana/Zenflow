package org.phong.zenflow.workflow.subdomain.engine.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
import org.springframework.context.ApplicationEventPublisher;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.engine.event.NodeCommitEvent;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WorkflowNavigatorServiceTest {

    @Test
    void haltedWaitNodeResumesAfterDependenciesCommit() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        WorkflowNavigatorService service = new WorkflowNavigatorService(publisher);

        BaseWorkflowNode waitNode = new BaseWorkflowNode(
                "wait", NodeType.PLUGIN,
                new PluginNodeIdentifier("core", "wait", "1.0.0", null),
                List.of(), new WorkflowConfig(), null, null
        );

        Map<String, Object> output = Map.of(
                "waitingNodes", Map.of("A", false, "B", false),
                "mode", "all",
                "threshold", 1
        );
        ExecutionResult result = ExecutionResult.uncommit(output);

        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        service.handleExecutionResult(workflowId, runId, waitNode, result, new WorkflowNodes(), new RuntimeContext());

        service.onNodeCommit(new NodeCommitEvent(workflowId, runId, "A"));
        verify(publisher, never()).publishEvent(any());

        service.onNodeCommit(new NodeCommitEvent(workflowId, runId, "B"));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        WorkflowTriggerEvent event = (WorkflowTriggerEvent) captor.getValue();
        assertEquals(runId, event.getWorkflowRunId());
        assertEquals("wait", event.request().startFromNodeKey());
    }
}
