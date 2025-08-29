package org.phong.zenflow.workflow.subdomain.engine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_execution.service.NodeExecutionService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowEngineService - UUID Integration Tests")
class WorkflowEngineServiceUuidIntegrationTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private NodeExecutionService nodeExecutionService;

    @Mock
    private WorkflowValidationService workflowValidationService;

    @Mock
    private PluginNodeExecutorDispatcher executorDispatcher;

    @Mock
    private WorkflowNavigatorService workflowNavigatorService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private RuntimeContextManager contextManager;

    @Mock
    private RuntimeContext runtimeContext;

    private WorkflowEngineService workflowEngineService;

    private UUID workflowId;
    private UUID workflowRunId;
    private UUID testNodeId1;
    private UUID testNodeId2;

    @BeforeEach
    void setUp() {
        workflowEngineService = new WorkflowEngineService(
                workflowRepository,
                nodeExecutionService,
                workflowValidationService,
                executorDispatcher,
                workflowNavigatorService,
                publisher,
                contextManager
        );

        workflowId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        workflowRunId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        testNodeId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        testNodeId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");
    }

    @Test
    @DisplayName("Should execute workflow using UUID-based node identification")
    void shouldExecuteWorkflowWithUuidNodes() {
        // Arrange
        PluginNodeIdentifier identifier1 = createPluginNodeIdentifier("email", "send", "1.0.0", testNodeId1);
        BaseWorkflowNode node1 = createWorkflowNode("node1", identifier1);

        WorkflowDefinition definition = new WorkflowDefinition(new WorkflowNodes(List.of(node1)), new WorkflowMetadata());
        Workflow workflow = createWorkflow(workflowId, definition);

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));
        when(executorDispatcher.dispatch(eq(testNodeId1.toString()), eq("node1"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("result", "success")));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Mock runtime context
        when(runtimeContext.resolveConfig(anyString(), any())).thenReturn(new WorkflowConfig(Map.of(), Map.of()));

        // Act
        var result = workflowEngineService.runWorkflow(workflowId, workflowRunId, "node1", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);

        // Verify UUID was used for execution dispatch
        verify(executorDispatcher).dispatch(eq(testNodeId1.toString()), eq("node1"), any(), any());
        verify(workflowValidationService).validateRuntime(eq("node1"), any(), eq(testNodeId1.toString()), any());
    }

    @Test
    @DisplayName("Should fall back to composite key when UUID is not available")
    void shouldFallBackToCompositeKeyWhenUuidNotAvailable() {
        // Arrange
        PluginNodeIdentifier identifier = createPluginNodeIdentifier("email", "send", "1.0.0", null);
        BaseWorkflowNode node = createWorkflowNode("node1", identifier);

        WorkflowDefinition definition = new WorkflowDefinition(new WorkflowNodes(List.of(node)), new WorkflowMetadata());
        Workflow workflow = createWorkflow(workflowId, definition);

        String expectedCompositeKey = "email:send:1.0.0";

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));
        when(executorDispatcher.dispatch(eq(expectedCompositeKey), eq("node1"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("result", "success")));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Mock runtime context
        when(runtimeContext.resolveConfig(anyString(), any())).thenReturn(new WorkflowConfig(Map.of(), Map.of()));

        // Act
        var result = workflowEngineService.runWorkflow(workflowId, workflowRunId, "node1", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);

        // Verify composite key was used for execution dispatch as fallback
        verify(executorDispatcher).dispatch(eq(expectedCompositeKey), eq("node1"), any(), any());
        verify(workflowValidationService).validateRuntime(eq("node1"), any(), eq(expectedCompositeKey), any());
    }

    @Test
    @DisplayName("Should handle mixed UUID and composite key nodes in same workflow")
    void shouldHandleMixedUuidAndCompositeKeyNodes() {
        // Arrange
        PluginNodeIdentifier uuidIdentifier = createPluginNodeIdentifier("email", "send", "1.0.0", testNodeId1);
        PluginNodeIdentifier compositeIdentifier = createPluginNodeIdentifier("slack", "message", "2.1.0", null);

        BaseWorkflowNode uuidNode = createWorkflowNode("uuid-node", uuidIdentifier);
        BaseWorkflowNode compositeNode = createWorkflowNode("composite-node", compositeIdentifier);

        // Set up proper node flow: uuid-node -> composite-node
        uuidNode.setNext(List.of("composite-node"));
        compositeNode.setNext(List.of()); // End of workflow

        WorkflowDefinition definition = new WorkflowDefinition(
                new WorkflowNodes(Arrays.asList(uuidNode, compositeNode)),
                new WorkflowMetadata()
        );
        Workflow workflow = createWorkflow(workflowId, definition);

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));

        // Mock both UUID and composite key executions
        when(executorDispatcher.dispatch(eq(testNodeId1.toString()), eq("uuid-node"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("step", 1)));
        when(executorDispatcher.dispatch(eq("slack:message:2.1.0"), eq("composite-node"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("step", 2)));

        // Mock workflow navigation to proceed from uuid-node to composite-node
        when(workflowNavigatorService.handleExecutionResult(eq(workflowId), eq(workflowRunId),
                eq(uuidNode), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(compositeNode,
                        WorkflowExecutionStatus.HALTED));

        when(workflowNavigatorService.handleExecutionResult(eq(workflowId), eq(workflowRunId),
                eq(compositeNode), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Mock runtime context
        when(runtimeContext.resolveConfig(anyString(), any())).thenReturn(new WorkflowConfig(Map.of(), Map.of()));

        // Act
        var result = workflowEngineService.runWorkflow(workflowId, workflowRunId, "uuid-node", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);

        // Verify both UUID and composite key approaches were used appropriately
        verify(executorDispatcher).dispatch(eq(testNodeId1.toString()), eq("uuid-node"), any(), any());
        verify(executorDispatcher).dispatch(eq("slack:message:2.1.0"), eq("composite-node"), any(), any());

        verify(workflowValidationService).validateRuntime(eq("uuid-node"), any(), eq(testNodeId1.toString()), any());
        verify(workflowValidationService).validateRuntime(eq("composite-node"), any(), eq("slack:message:2.1.0"), any());
    }

    @Test
    @DisplayName("Should process workflow context with UUID-resolved outputs")
    void shouldProcessWorkflowContextWithUuidResolvedOutputs() {
        // Arrange
        PluginNodeIdentifier identifier = createPluginNodeIdentifier("data", "transformer", "1.5.0", testNodeId2);
        BaseWorkflowNode node = createWorkflowNode("transform-node", identifier);

        WorkflowDefinition definition = new WorkflowDefinition(new WorkflowNodes(List.of(node)), new WorkflowMetadata());
        Workflow workflow = createWorkflow(workflowId, definition);

        Map<String, Object> expectedOutput = Map.of(
                "transformedData", "processed",
                "nodeId", testNodeId2.toString(),
                "timestamp", System.currentTimeMillis()
        );

        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));
        when(executorDispatcher.dispatch(eq(testNodeId2.toString()), eq("transform-node"), any(), any()))
                .thenReturn(ExecutionResult.success(expectedOutput));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Mock runtime context to capture output processing
        when(runtimeContext.resolveConfig(anyString(), any())).thenReturn(new WorkflowConfig(Map.of(), Map.of()));
        doNothing().when(runtimeContext).processOutputWithMetadata(anyString(), any());

        // Act
        var result = workflowEngineService.runWorkflow(workflowId, workflowRunId, "transform-node", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);

        // Verify UUID was used for execution
        verify(executorDispatcher).dispatch(eq(testNodeId2.toString()), eq("transform-node"), any(), any());

        // Verify output was processed with metadata
        verify(runtimeContext).processOutputWithMetadata(eq("transform-node.output"), eq(expectedOutput));

        // Verify node execution tracking
        verify(nodeExecutionService).startNode(workflowRunId, "transform-node");
        verify(nodeExecutionService).resolveNodeExecution(eq(workflowId), eq(workflowRunId), eq(node), any());
    }

    // Helper methods
    private PluginNodeIdentifier createPluginNodeIdentifier(String pluginKey, String nodeKey, String version, UUID nodeId) {
        return new PluginNodeIdentifier(nodeId, pluginKey, nodeKey, version, "builtin");
    }

    private BaseWorkflowNode createWorkflowNode(String key, PluginNodeIdentifier identifier) {
        return new BaseWorkflowNode(
                key,
                NodeType.ACTION,
                identifier,
                List.of(),
                new WorkflowConfig(Map.of(), Map.of()),
                Map.of(),
                Map.of()
        );
    }

    private Workflow createWorkflow(UUID id, WorkflowDefinition definition) {
        Workflow workflow = new Workflow();
        workflow.setId(id);
        workflow.setDefinition(definition);
        return workflow;
    }
}
