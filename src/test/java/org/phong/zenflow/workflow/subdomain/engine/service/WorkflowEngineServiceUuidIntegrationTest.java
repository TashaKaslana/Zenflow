package org.phong.zenflow.workflow.subdomain.engine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.services.NodeExecutorDispatcher;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.context.resolution.SystemLoadMonitor;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.evaluator.functions.string.StringContainsFunction;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_execution.service.NodeExecutionService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.phong.zenflow.workflow.subdomain.worker.ExecutionTaskRegistry;
import org.phong.zenflow.workflow.subdomain.worker.gateway.ExecutionGatewayImpl;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
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
    private NodeExecutorDispatcher executorDispatcher;

    @Mock
    private WorkflowNavigatorService workflowNavigatorService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private RuntimeContextManager contextManager;

    @Mock
    private RuntimeContext runtimeContext;

    @Mock
    private AuthService authService;

    @Mock
    private ExecutionTaskRegistry taskRegistry;

    private WorkflowEngineService workflowEngineService;

    private UUID workflowId;
    private UUID workflowRunId;
    private UUID testNodeId1;
    private UUID testNodeId2;

    @BeforeEach
    void setUp() {
        TemplateService templateService = new TemplateService(new AviatorFunctionRegistry(List.of(new StringContainsFunction())));
        // Direct executor for testing
        ExecutionGatewayImpl executionGateway = new ExecutionGatewayImpl(
                taskRegistry,
                executorDispatcher,
                Runnable::run // Direct executor for testing
        );
        ContextValueResolver contextValueResolver = new ContextValueResolver(new SystemLoadMonitor());
        workflowEngineService = new WorkflowEngineService(
                nodeExecutionService,
                executionGateway,
                workflowNavigatorService,
                publisher,
                contextManager,
                templateService,
                authService,
                contextValueResolver
        );

        workflowId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        workflowRunId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        testNodeId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        testNodeId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

        // Fix: Add proper stubbing for RuntimeContextManager to return a mock RuntimeContext
        when(contextManager.getOrCreate(anyString())).thenReturn(runtimeContext);
        when(taskRegistry.registerTask(any(), any())).thenReturn(true);
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
        when(executorDispatcher.dispatch(any(ExecutionTaskEnvelope.class)))
                .thenReturn(ExecutionResult.success(Map.of("result", "success")));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        // Act
        var result = workflowEngineService.runWorkflow(workflow, workflowRunId, "node1", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);
        ArgumentCaptor<ExecutionTaskEnvelope> envelopeCaptor = ArgumentCaptor.forClass(ExecutionTaskEnvelope.class);
        verify(executorDispatcher).dispatch(envelopeCaptor.capture());
        assertEquals(testNodeId1.toString(), envelopeCaptor.getValue().getExecutorIdentifier());
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
        when(executorDispatcher.dispatch(any(ExecutionTaskEnvelope.class)))
                .thenReturn(ExecutionResult.success(expectedOutput));
        when(workflowNavigatorService.handleExecutionResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new WorkflowNavigatorService.ExecutionStepOutcome(null,
                        WorkflowExecutionStatus.COMPLETED));

        doNothing().when(runtimeContext).processOutputWithMetadata(anyString(), any());

        // Act
        var result = workflowEngineService.runWorkflow(workflow, workflowRunId, "transform-node", runtimeContext);

        // Assert
        assertEquals(WorkflowExecutionStatus.COMPLETED, result);

        ArgumentCaptor<ExecutionTaskEnvelope> envelopeCaptor = ArgumentCaptor.forClass(ExecutionTaskEnvelope.class);
        verify(executorDispatcher).dispatch(envelopeCaptor.capture());
        assertEquals(testNodeId2.toString(), envelopeCaptor.getValue().getExecutorIdentifier());
        
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
