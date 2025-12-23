package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionContextImplTest {

    @Mock
    private NodeExecutionOrchestrator orchestrator;
    @Mock
    private RuntimeContextManager contextManager;
    @Mock
    private BaseWorkflowNode node;

    private ExecutionContextImpl executionContext;

    @BeforeEach
    void setUp() {
        executionContext = ExecutionContextImpl.builder()
                .orchestrator(orchestrator)
                .contextManager(contextManager)
                .build();
    }

    @Test
    void executeSubNode_CapturesOutputFromWrite() {
        // Arrange
        WorkflowConfig config = new WorkflowConfig();
        
        // Mock orchestrator to simulate node execution writing to context
        when(orchestrator.executeNode(eq(node), eq(config), eq(executionContext))).thenAnswer(invocation -> {
            // Simulate the node writing to the context
            executionContext.write("outputKey", "capturedValue");
            
            ExecutionResult result = new ExecutionResult();
            result.setStatus(ExecutionStatus.SUCCESS);
            return result;
        });

        // Act
        ExecutionResult result = executionContext.executeSubNode(node, config);

        // Assert
        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.getOutputPayload()).isNotNull();
        assertThat((Map<String, Object>) result.getOutputPayload()).containsEntry("outputKey", "capturedValue");
        
        // Verify capture mode was reset
        assertThat(executionContext.isCaptureMode()).isFalse();
        assertThat(executionContext.getCapturedOutputs()).isEmpty();
    }

    @Test
    void executeSubNode_PrioritizesDirectPayload() {
        // Arrange
        WorkflowConfig config = new WorkflowConfig();
        Map<String, Object> directPayload = Map.of("direct", "payload");
        
        when(orchestrator.executeNode(eq(node), eq(config), eq(executionContext))).thenAnswer(invocation -> {
            // Simulate write AND return payload
            executionContext.write("ignoredKey", "ignoredValue");
            
            ExecutionResult res = new ExecutionResult();
            res.setStatus(ExecutionStatus.SUCCESS);
            res.setOutputPayload(directPayload);
            return res;
        });

        // Act
        ExecutionResult result = executionContext.executeSubNode(node, config);

        // Assert
        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(result.getOutputPayload()).isEqualTo(directPayload);
        assertThat((Map<String, Object>) result.getOutputPayload()).doesNotContainKey("ignoredKey");
    }

    @Test
    void executeSubNode_HandlesFailure() {
        // Arrange
        WorkflowConfig config = new WorkflowConfig();
        
        ExecutionResult errorResult = new ExecutionResult();
        errorResult.setStatus(ExecutionStatus.ERROR);
        errorResult.setError("Failed");

        when(orchestrator.executeNode(eq(node), eq(config), eq(executionContext))).thenReturn(errorResult);

        // Act
        ExecutionResult result = executionContext.executeSubNode(node, config);

        // Assert
        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(result.getError()).isEqualTo("Failed");
    }
}
