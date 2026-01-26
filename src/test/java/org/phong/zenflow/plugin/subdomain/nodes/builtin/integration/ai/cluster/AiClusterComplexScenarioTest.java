package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiExecutionContextKeys;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.factory.AiExecutionRequestFactory;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools.NodeToolCallback;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.util.WorkflowNodeKeyUtils;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiClusterComplexScenarioTest {

    @Mock
    private NodeExecutionOrchestrator orchestrator;
    @Mock
    private AiExecutionRequestFactory requestFactory;
    @Mock
    private AiToolRegistry toolRegistry;
    @Mock
    private AiClusterProviderResolver providerResolver;
    @Mock
    private ExecutionContext context;
    @Mock
    private NodeLogPublisher logPublisher;

    private ObjectMapper objectMapper = new ObjectMapper();
    private AiClusterExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AiClusterExecutor(requestFactory, toolRegistry, objectMapper, providerResolver);
        when(context.getLogPublisher()).thenReturn(logPublisher);
        when(toolRegistry.copy()).thenReturn(new AiToolRegistry(new ArrayList<>()));
    }

    @Test
    void testAiClusterWithMemoryAndTools() {
        // 1. Setup Context and Configuration
        String clusterNodeKey = "ai_cluster_node";
        String toolNodeKey = "weather_tool";
        String memoryKey = "chat_history";

        // Mock Context Reads for Config
        when(context.read("prompt", String.class)).thenReturn("What is the weather in Tokyo?");
        when(context.readOrDefault("system_prompt", String.class, null)).thenReturn("You are a helpful assistant.");
        when(context.readOrDefault("response_format", String.class, "text")).thenReturn("text");
        when(context.readOrDefault("provider", String.class, null)).thenReturn("openai");
        when(context.readOrDefault("model", String.class, null)).thenReturn("gpt-4");
        when(context.readOrDefault("model_options", Map.class, new HashMap<>())).thenReturn(new HashMap<>());
        
        // Memory Config
        when(context.readOrDefault("memory_key", String.class, null)).thenReturn(memoryKey);
        when(context.readOrDefault("include_history", Boolean.class, false)).thenReturn(true);
        when(context.readOrDefault("max_history_messages", Integer.class, 10)).thenReturn(5);
        when(context.readOrDefault("memory", Map.class, new HashMap<>())).thenReturn(Map.of("backend", "context"));

        // Tool Config
        when(context.readOrDefault("tools", Map.class, new HashMap<>())).thenReturn(Map.of("enabled", true));
        when(context.readOrDefault("parser", Map.class, new HashMap<>())).thenReturn(new HashMap<>());

        // 2. Setup Node Structure (Cluster + Tool Child)
        when(context.getNodeKey()).thenReturn(clusterNodeKey);
        
        BaseWorkflowNode clusterNode = mock(BaseWorkflowNode.class);
        String providerChildKey = WorkflowNodeKeyUtils.buildChildKey(clusterNodeKey, "openai");
        when(clusterNode.getChildNodeKeys()).thenReturn(List.of(toolNodeKey, providerChildKey));
        when(context.getWorkflowNode(clusterNodeKey)).thenReturn(clusterNode);

        BaseWorkflowNode toolNode = mock(BaseWorkflowNode.class);
        when(toolNode.getKey()).thenReturn(toolNodeKey);
        when(context.getWorkflowNode(toolNodeKey)).thenReturn(toolNode);

        // 3. Setup Provider Resolution
        AiClusterProviderResolver.ProviderInfo providerInfo = new AiClusterProviderResolver.ProviderInfo(
            "openai", "chatgpt", "1.0.0", "builtin", Set.of("openai", "gpt-4"), "openai"
        );
        when(providerResolver.resolve("openai")).thenReturn(Optional.of(providerInfo));

        // 4. Setup Request Factory
        AiExecutionRequest mockRequest = mock(AiExecutionRequest.class);
        when(mockRequest.getMessages()).thenReturn(List.of(new UserMessage("What is the weather in Tokyo?")));
        when(requestFactory.build(any(), anyList(), anyList())).thenReturn(mockRequest);

        // 5. Setup Provider Execution (Mocking the actual LLM call)
        // The executor calls orchestrator.executeNode() for the provider.
        // We need to intercept this and return a successful result.
        
        // Mock the provider node lookup
        // The child key is built using WorkflowNodeKeyUtils.buildChildKey(context.getNodeKey(), descriptor.childAlias())
        // context.getNodeKey() = "ai_cluster_node"
        // descriptor.childAlias() = "openai" (from our mock ProviderInfo)
        // So childKey = "ai_cluster_node:openai" (assuming standard separator)
        // Let's verify WorkflowNodeKeyUtils logic or just match the key used in the code.
        // The code uses WorkflowNodeKeyUtils.buildChildKey.
        // Assuming it joins with ":".
        
        BaseWorkflowNode providerNode = mock(BaseWorkflowNode.class);
        when(context.getWorkflowNode(providerChildKey)).thenReturn(providerNode);

        // Mock the orchestrator execution
        // IMPORTANT: The code calls providerNode.setConfig(providerConfig) BEFORE executeNode.
        // Mockito might be strict about the object state if we match 'eq(providerNode)'.
        // But 'providerNode' is a mock, so it's fine.
        
        when(orchestrator.executeNode(eq(providerNode), any(WorkflowConfig.class), eq(context)))
                .thenAnswer(invocation -> {
                    // Simulate the provider writing to the context
                    // The real provider writes to the context.
                    // But since 'context' is a mock, the writes don't persist to be read later.
                    // We have to mock the READS separately (which we did below).
                    
                    ExecutionResult res = new ExecutionResult();
                    res.setStatus(ExecutionStatus.SUCCESS);
                    return res;
                });
        
        // Mock context reads for the provider output (since we just mocked the write, we need to mock the read back)
        // In a real integration test with a real context, this wouldn't be needed.
        // But here 'context' is a mock.
        when(context.read("response", Object.class)).thenReturn("The weather in Tokyo is 25C.");
        when(context.read("raw_response", String.class)).thenReturn("{\"choices\": [{\"message\": {\"content\": \"The weather in Tokyo is 25C.\"}}]}");
        when(context.read("provider", String.class)).thenReturn("openai");
        when(context.read("metadata", Map.class)).thenReturn(Map.of("usage", Map.of("total_tokens", 100)));
        when(context.read("usage", Object.class)).thenReturn(Map.of("total_tokens", 100));

        // 6. Execute
        ExecutionResult result = executor.execute(context);

        // 7. Assertions
        assertThat(result.getStatus())
            .as("ExecutionResult error: %s", result.getError())
            .isEqualTo(ExecutionStatus.SUCCESS);
        
        // Verify Memory Interaction
        // The executor should have loaded history (we mocked empty) and saved the turn.
        // Since we mocked the backend as CONTEXT, it uses ContextMemoryBackend.
        // We should verify that context.write was called to save the conversation.
        // ContextMemoryBackend writes to the memoryKey.
        
        // Verify that the tool was discovered (passed to requestFactory)
        verify(requestFactory).build(any(), argThat(tools -> {
            // Check if our tool node is in the list
            return tools.stream().anyMatch(t -> t instanceof NodeToolCallback);
        }), anyList());
        
        // Verify output writes
        verify(context).write("response", "The weather in Tokyo is 25C.");
        verify(context).write(AiExecutionContextKeys.USAGE_KEY, Map.of("total_tokens", 100));
    }
}
