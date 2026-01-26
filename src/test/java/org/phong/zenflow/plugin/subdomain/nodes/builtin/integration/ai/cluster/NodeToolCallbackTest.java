package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools.NodeToolCallback;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeToolCallbackTest {

    @Mock
    private BaseWorkflowNode node;
    @Mock
    private ExecutionContext context;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    private NodeToolCallback callback;
    private WorkflowConfig baseConfig;

    @BeforeEach
    void setUp() {
        when(node.getKey()).thenReturn("test-node-key");
        baseConfig = new WorkflowConfig(
                Map.of(
                        "method", "GET",
                        "url", "https://api.example.com/weather"
                ),
                List.of("DefaultProfile"),
                Map.of("existing", "value")
        );
        when(node.getConfig()).thenReturn(baseConfig);
        callback = new NodeToolCallback(node, context, objectMapper);
    }

    @Test
    void callExecutesSubNodeAndReturnsPayload() throws JsonProcessingException {
        // Arrange
        String inputJson = "{\"param1\": \"value1\"}";
        Map<String, Object> expectedPayload = Map.of("result", "success_data");
        
        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setStatus(ExecutionStatus.SUCCESS);
        mockResult.setOutputPayload(expectedPayload);

        when(context.executeSubNode(eq(node), any(WorkflowConfig.class))).thenReturn(mockResult);

        // Act
        String resultJson = callback.call(inputJson);

        // Assert
        // 1. Verify sub-node execution
        ArgumentCaptor<WorkflowConfig> configCaptor = ArgumentCaptor.forClass(WorkflowConfig.class);
        verify(context).executeSubNode(eq(node), configCaptor.capture());
        
        WorkflowConfig capturedConfig = configCaptor.getValue();
        assertThat(capturedConfig.input())
            .containsEntry("param1", "value1")
            .containsEntry("method", "GET")
            .containsEntry("url", "https://api.example.com/weather");

        // 2. Verify return value
        Map<String, Object> resultMap = objectMapper.readValue(resultJson, Map.class);
        assertThat(resultMap).containsEntry("result", "success_data");
    }

    @Test
    void callReturnsDefaultSuccessMessageIfNoPayload() throws JsonProcessingException {
        // Arrange
        String inputJson = "{}";
        
        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setStatus(ExecutionStatus.SUCCESS);
        mockResult.setOutputPayload(null); // No payload

        when(context.executeSubNode(eq(node), any(WorkflowConfig.class))).thenReturn(mockResult);

        // Act
        String resultJson = callback.call(inputJson);

        // Assert
        Map<String, Object> resultMap = objectMapper.readValue(resultJson, Map.class);
        assertThat(resultMap).containsEntry("status", "success");
        assertThat(resultMap).containsKey("message");
    }

    @Test
    void callReturnsErrorOnFailure() throws JsonProcessingException {
        // Arrange
        String inputJson = "{}";
        
        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setStatus(ExecutionStatus.ERROR);
        mockResult.setError("Something went wrong");

        when(context.executeSubNode(eq(node), any(WorkflowConfig.class))).thenReturn(mockResult);

        // Act
        String resultJson = callback.call(inputJson);

        // Assert
        Map<String, Object> resultMap = objectMapper.readValue(resultJson, Map.class);
        assertThat(resultMap).containsEntry("status", "error");
        assertThat(resultMap).containsEntry("message", "Something went wrong");
    }

    @Test
    void callOverridesExistingConfigValues() throws JsonProcessingException {
        // Arrange
        String inputJson = "{\"method\": \"POST\", \"city\": \"Tokyo\"}";

        ExecutionResult mockResult = new ExecutionResult();
        mockResult.setStatus(ExecutionStatus.SUCCESS);
        when(context.executeSubNode(eq(node), any(WorkflowConfig.class))).thenReturn(mockResult);

        // Act
        callback.call(inputJson);

        // Assert
        ArgumentCaptor<WorkflowConfig> configCaptor = ArgumentCaptor.forClass(WorkflowConfig.class);
        verify(context).executeSubNode(eq(node), configCaptor.capture());
        WorkflowConfig mergedConfig = configCaptor.getValue();

        assertThat(mergedConfig.input())
                .containsEntry("method", "POST")
                .containsEntry("url", "https://api.example.com/weather")
                .containsEntry("city", "Tokyo");
        assertThat(mergedConfig.profile()).containsExactly("DefaultProfile");
    }
}
