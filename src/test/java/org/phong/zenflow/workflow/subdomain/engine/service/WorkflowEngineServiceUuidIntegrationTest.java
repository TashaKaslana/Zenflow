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
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.execution.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.execution.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.execution.functions.StringContainsFunction;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkflowEngineService - UUID Integration Tests")
class WorkflowEngineServiceUuidIntegrationTest {

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
    private TemplateService templateService;

    private UUID workflowId;
    private UUID workflowRunId;
    private UUID testNodeId1;
    private UUID testNodeId2;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(new AviatorFunctionRegistry(List.of(new StringContainsFunction())));
        workflowEngineService = new WorkflowEngineService(
                nodeExecutionService,
                workflowValidationService,
                executorDispatcher,
                workflowNavigatorService,
                publisher,
                contextManager,
                templateService
        );

        workflowId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        workflowRunId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        testNodeId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        testNodeId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

        // Fix: Add proper stubbing for RuntimeContextManager to return a mock RuntimeContext
        when(contextManager.getOrCreate(anyString())).thenReturn(runtimeContext);
    }

    @Test
    @DisplayName("Should execute workflow using UUID-based node identification")
    void shouldExecuteWorkflowWithUuidNodes() {
        // Arrange
        PluginNodeIdentifier identifier1 = createPluginNodeIdentifier("email", "send", "1.0.0", testNodeId1);
        BaseWorkflowNode node1 = createWorkflowNode("node1", identifier1);

        WorkflowDefinition definition = new WorkflowDefinition(new WorkflowNodes(List.of(node1)), new WorkflowMetadata());
        Workflow workflow = createWorkflow(workflowId, definition);

        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));
        when(executorDispatcher.dispatch(eq(testNodeId1.toString()), eq("builtin"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("result", "success")));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Act
        var result = workflowEngineService.runWorkflow(workflow, workflowRunId, "node1", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);
        verify(executorDispatcher).dispatch(eq(testNodeId1.toString()), eq("builtin"), any(), any());
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

        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));
        when(executorDispatcher.dispatch(eq(expectedCompositeKey), eq("builtin"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("result", "success")));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Act
        var result = workflowEngineService.runWorkflow(workflow, workflowRunId, "node1", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);
        verify(executorDispatcher).dispatch(eq(expectedCompositeKey), eq("builtin"), any(), any());
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

        uuidNode.setNext(List.of("composite-node"));
        compositeNode.setNext(List.of());

        WorkflowDefinition definition = new WorkflowDefinition(
                new WorkflowNodes(Arrays.asList(uuidNode, compositeNode)),
                new WorkflowMetadata()
        );
        Workflow workflow = createWorkflow(workflowId, definition);

        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));

        when(executorDispatcher.dispatch(eq(testNodeId1.toString()), eq("builtin"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("step", 1)));
        when(executorDispatcher.dispatch(eq("slack:message:2.1.0"), eq("builtin"), any(), any()))
                .thenReturn(ExecutionResult.success(Map.of("step", 2)));

        when(workflowNavigatorService.handleExecutionResult(eq(workflowId), eq(workflowRunId),
                eq(uuidNode), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(compositeNode,
                        WorkflowExecutionStatus.HALTED));

        when(workflowNavigatorService.handleExecutionResult(eq(workflowId), eq(workflowRunId),
                eq(compositeNode), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Act
        var result = workflowEngineService.runWorkflow(workflow, workflowRunId, "uuid-node", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);

        verify(executorDispatcher).dispatch(eq(testNodeId1.toString()), eq("builtin"), any(), any());
        verify(executorDispatcher).dispatch(eq("slack:message:2.1.0"), eq("builtin"), any(), any());

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

        when(workflowValidationService.validateRuntime(any(), any(), any(), any()))
                .thenReturn(new ValidationResult("runtime", List.of()));
        when(executorDispatcher.dispatch(eq(testNodeId2.toString()), eq("builtin"), any(), any()))
                .thenReturn(ExecutionResult.success(expectedOutput));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        doNothing().when(runtimeContext).processOutputWithMetadata(anyString(), any());

        // Act
        var result = workflowEngineService.runWorkflow(workflow, workflowRunId, "transform-node", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);

        verify(executorDispatcher).dispatch(eq(testNodeId2.toString()), eq("builtin"), any(), any());
        verify(runtimeContext).processOutputWithMetadata(eq("transform-node.output"), eq(expectedOutput));
        verify(nodeExecutionService).startNode(workflowRunId, "transform-node");
        verify(nodeExecutionService).resolveNodeExecution(eq(workflowId), eq(workflowRunId), eq(node), any(), isNull());
    }

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
