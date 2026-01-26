package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.memory.MemoryExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiExecutionContextKeys;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.factory.AiExecutionRequestFactory;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.WriteOptions;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for AI Cluster executor.
 * Tests the full flow with REAL memory node and MOCKED provider (no API calls).
 * Verifies actual data passing between cluster → memory → provider.
 */
class AiClusterExecutorTest {

    private NodeExecutionOrchestrator orchestrator;
    private ExecutionContext context;
    private NodeLogPublisher logPublisher;

    private AiClusterExecutor executor;
    private MemoryExecutor memoryExecutor; // Real memory node!
    private AiToolRegistry toolRegistry;
    private AiExecutionRequestFactory requestFactory;
    private ObjectMapper objectMapper;
    private AiClusterProviderResolver providerResolver;
        private Map<String, Object> contextStorage;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        toolRegistry = new AiToolRegistry(Collections.emptyList());
        requestFactory = new AiExecutionRequestFactory();
        
        // Real memory executor!
        memoryExecutor = new MemoryExecutor();
        
        // Mock orchestrator to route calls to real memory or mock provider
        orchestrator = mock(NodeExecutionOrchestrator.class);
        
        // Mock context and logger
        context = mock(ExecutionContext.class);
        logPublisher = mock(NodeLogPublisher.class);
        
        providerResolver = new AiClusterProviderResolver();
        executor = new AiClusterExecutor(requestFactory, toolRegistry, objectMapper, providerResolver);
        lenient().when(context.getWorkflowNode(anyString())).thenReturn(null);

        // Setup orchestrator to delegate memory operations to REAL MemoryExecutor
        when(orchestrator.executeSyntheticNodeByKey(
                eq("core:context_variable:1.0.0"),
                anyString(),
                any(WorkflowConfig.class),
                eq(context)
        )).thenAnswer(invocation -> {
            // Extract memory operation config and execute with REAL memory executor
            WorkflowConfig memoryConfig = invocation.getArgument(2);
            
            // Setup context inputs for memory operation
            when(context.read("operation", String.class)).thenReturn((String) memoryConfig.input().get("operation"));
            when(context.read("key", String.class)).thenReturn((String) memoryConfig.input().get("key"));
            when(context.read("value", Object.class)).thenReturn(memoryConfig.input().get("value"));
            when(context.readOrDefault("default_value", Object.class, null))
                    .thenReturn(memoryConfig.input().getOrDefault("default_value", null));
            when(context.readOrDefault("overwrite", Boolean.class, true)).thenReturn(true);
            when(context.readOrDefault("persistent", Boolean.class, false))
                    .thenReturn(Boolean.TRUE.equals(memoryConfig.input().getOrDefault("persistent", false)));

            // Execute REAL memory node
            return memoryExecutor.execute(context);
        });
        
        // Setup common mocks with lenient stubbing for logger (called with various messages)
        lenient().when(context.getLogPublisher()).thenReturn(logPublisher);
        lenient().doNothing().when(logPublisher).info(anyString(), any());
        lenient().doNothing().when(logPublisher).info(anyString());
        lenient().doNothing().when(logPublisher).success(anyString(), any());
        lenient().doNothing().when(logPublisher).success(anyString());
        lenient().doNothing().when(logPublisher).warn(anyString(), any());
        lenient().doNothing().when(logPublisher).warn(anyString());
        lenient().doNothing().when(logPublisher).error(anyString(), any());
        
        // Allow writing to context (storing memory state)
        contextStorage = new HashMap<>();
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            contextStorage.put(key, value);
            return null;
        }).when(context).write(anyString(), any());
        
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            contextStorage.put(key, value);
            return null;
        }).when(context).write(anyString(), any(), any(WriteOptions.class));
        
        // Allow reading from context (retrieving memory state)
        lenient().when(context.read(anyString(), eq(Object.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return contextStorage.get(key);
        });
        
        lenient().when(context.read(anyString(), eq(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = contextStorage.get(key);
            return value != null ? value.toString() : null;
        });

        lenient().when(context.read(anyString(), eq(Map.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = contextStorage.get(key);
            return value instanceof Map ? value : null;
        });

        lenient().when(context.read(anyString(), eq(AiExecutionRequest.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return (AiExecutionRequest) contextStorage.get(key);
        });

        lenient().when(context.containsKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return contextStorage.containsKey(key);
        });

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            contextStorage.remove(key);
            return null;
        }).when(context).remove(anyString());
        
        // Setup orchestrator to mock provider responses (no real API calls)
        when(orchestrator.executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        )).thenAnswer(invocation -> {
            // Simulate provider response by writing to context
            contextStorage.put("response", "This is a mocked AI response");
            contextStorage.put("raw_response", "This is a mocked AI response");
            contextStorage.put("provider", "gemini");
            contextStorage.put("metadata", Map.of(
                    "usage", Map.of(
                            "prompt_tokens", 10,
                            "completion_tokens", 5,
                            "total_tokens", 15
                    ),
                    "latency_ms", 123L
            ));
            return ExecutionResult.success();
        });
    }
    
    /**
     * Setup basic cluster configuration on context
     */
    private void setupBasicConfig(String prompt, String provider, boolean includeHistory, String memoryKey) {
        when(context.read("prompt", String.class)).thenReturn(prompt);
        when(context.readOrDefault("system_prompt", String.class, null)).thenReturn(null);
        when(context.readOrDefault("response_format", String.class, "text")).thenReturn("text");
        when(context.readOrDefault(eq("provider"), eq(String.class), any())).thenReturn(provider);
        when(context.readOrDefault("model", String.class, null)).thenReturn(null);
        when(context.readOrDefault("model_options", Map.class, new HashMap<>())).thenReturn(new HashMap<>());
        when(context.readOrDefault("memory_key", String.class, null)).thenReturn(memoryKey);
        when(context.readOrDefault("include_history", Boolean.class, false)).thenReturn(includeHistory);
        when(context.readOrDefault("max_history_messages", Integer.class, 10)).thenReturn(10);
    }

    @Test
    void testBasicExecution_WithoutHistory() {
        // Given: Basic cluster config WITHOUT history
        setupBasicConfig("What is the weather?", "gemini", false, null);

        // When: Execute cluster
        ExecutionResult result = executor.execute(context);

        // Then: Should succeed
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());

        // Verify provider was called
        verify(orchestrator).executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        );

        // Verify outputs were written
        verify(context, atLeastOnce()).write(eq("response"), anyString());
        verify(context, atLeastOnce()).write(eq("raw_response"), anyString());

        // Verify NO memory operations (history disabled)
        verify(orchestrator, never()).executeSyntheticNodeByKey(
                eq("core:context_variable:1.0.0"),
                anyString(),
                any(WorkflowConfig.class),
                eq(context)
        );

        // Flattened metadata should be present
        assertTrue(contextStorage.containsKey("usage"));
        assertTrue(contextStorage.containsKey("metadata.latency_ms"));
        assertFalse(contextStorage.containsKey("metadata"));
        assertFalse(contextStorage.containsKey(AiExecutionContextKeys.TYPED_REQUEST));
    }

    @Test
    void testTypedRequestLifecycle() {
        setupBasicConfig("Explain gravity", "gemini", false, null);

        executor.execute(context);

        assertFalse(contextStorage.containsKey(AiExecutionContextKeys.TYPED_REQUEST),
            "Typed request should be removed after provider execution");
    }

    @Test
    void testExecution_WithConversationHistory_RealMemoryNode() {
        // Given: Cluster config with history enabled
        setupBasicConfig("What about tomorrow?", "gemini", true, "chat_history");
        
        // Pre-populate memory with REAL memory executor
        // This simulates previous conversation stored in memory
        when(context.read("operation", String.class)).thenReturn("STORE");
        when(context.read("key", String.class)).thenReturn("chat_history");
        when(context.read("value", Object.class)).thenReturn(List.of(
                Map.of("role", "user", "content", "What is the weather?"),
                Map.of("role", "assistant", "content", "The weather is sunny")
        ));
        when(context.readOrDefault("overwrite", Boolean.class, true)).thenReturn(true);
        
        // Store initial history using REAL memory executor
        memoryExecutor.execute(context);

        // When: Execute cluster (will retrieve history, call provider, save new turn)
        ExecutionResult result = executor.execute(context);

        // Then: Should succeed
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());

        // Verify history was retrieved via orchestrator → REAL memory node
        verify(orchestrator, atLeastOnce()).executeSyntheticNodeByKey(
                eq("core:context_variable:1.0.0"),
                eq("retrieve_history"),
                any(WorkflowConfig.class),
                eq(context)
        );

        // Verify provider was called
        verify(orchestrator).executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        );

        // Verify conversation was saved (2 append calls via REAL memory: user + assistant)
        verify(orchestrator, atLeast(2)).executeSyntheticNodeByKey(
                eq("core:context_variable:1.0.0"),
                eq("append_message"),
                any(WorkflowConfig.class),
                eq(context)
        );
        
        // Verify outputs were written
        verify(context, atLeastOnce()).write(eq("response"), anyString());
        verify(context, atLeastOnce()).write(eq("raw_response"), anyString());
    }

    @Test
    void testExecution_JsonResponseFormat() {
        // Given: Cluster config with JSON response format
        when(context.read("prompt", String.class)).thenReturn("Get weather data");
        when(context.readOrDefault("system_prompt", String.class, null)).thenReturn(null);
        when(context.readOrDefault("response_format", String.class, "text")).thenReturn("json");
        when(context.readOrDefault("provider", String.class, "gemini")).thenReturn("gemini");
        when(context.readOrDefault("model", String.class, null)).thenReturn(null);
        when(context.readOrDefault("model_options", Map.class, new HashMap<>())).thenReturn(new HashMap<>());
        when(context.readOrDefault("memory_key", String.class, null)).thenReturn(null);
        when(context.readOrDefault("include_history", Boolean.class, false)).thenReturn(false);
        when(context.readOrDefault("max_history_messages", Integer.class, 10)).thenReturn(10);

        // Mock provider to return JSON
        when(orchestrator.executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        )).thenAnswer(invocation -> {
            // Simulate JSON response
            Map<String, Object> jsonResponse = Map.of(
                    "temperature", 25,
                    "condition", "sunny",
                    "humidity", 60
            );
            when(context.read("response", Object.class)).thenReturn(jsonResponse);
            when(context.read("raw_response", String.class)).thenReturn("{\"temperature\":25}");
            when(context.read("provider", String.class)).thenReturn("gemini");
            return ExecutionResult.success();
        });

        // When: Execute cluster
        ExecutionResult result = executor.execute(context);

        // Then: Should succeed with JSON output
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());

        // Verify parse_success is true for JSON
        verify(context, atLeastOnce()).write(eq("parse_success"), eq(true));
    }

    @Test
    void testExecution_ProviderFailure() {
        // Given: Cluster config
        setupBasicConfig("Test prompt", "gemini", false, null);

        // Mock provider failure
        when(orchestrator.executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        )).thenReturn(ExecutionResult.error("API rate limit exceeded"));

        // When: Execute cluster
        ExecutionResult result = executor.execute(context);

        // Then: Should fail
        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Provider execution failed"));
        assertTrue(result.getError().contains("API rate limit exceeded"));

        // Verify error was logged
        verify(logPublisher, atLeastOnce()).error(anyString(), any());
    }

    @Test
    void testExecution_UnsupportedProvider() {
        // Given: Cluster config with unsupported provider
        setupBasicConfig("Test prompt", "openai:gpt-4", false, null);

        // When: Execute cluster
        ExecutionResult result = executor.execute(context);

        // Then: Should fail with unsupported provider error
        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Unsupported model") || 
                   result.getError().contains("Unknown model"));

        // Verify provider was never called
        verify(orchestrator, never()).executeSyntheticNodeByKey(
                eq("openai:gpt-4:1.0.0"),
                anyString(),
                any(WorkflowConfig.class),
                eq(context)
        );
    }

    @Test
    void testExecution_MemoryRetrievalFailure() {
        // Given: Cluster config with history enabled
        setupBasicConfig("Follow-up question", "gemini", true, "chat_history");

        // Override orchestrator to fail on memory retrieval
        when(orchestrator.executeSyntheticNodeByKey(
                eq("core:context_variable:1.0.0"),
                eq("retrieve_history"),
                any(WorkflowConfig.class),
                eq(context)
        )).thenReturn(ExecutionResult.error("Memory not found"));

        // When: Execute cluster
        ExecutionResult result = executor.execute(context);

        // Then: Should succeed (gracefully handle memory failure)
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());

        // Verify warning was logged about memory failure
        verify(logPublisher, atLeastOnce()).warn(contains("Failed to retrieve conversation history"), any());

        // Verify provider was still called (with empty history)
        verify(orchestrator).executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        );
    }

    @Test
    void testExecution_WithModelVariant() {
        // Given: Cluster config with specific model variant
        when(context.read("prompt", String.class)).thenReturn("Test prompt");
        when(context.readOrDefault("system_prompt", String.class, null)).thenReturn(null);
        when(context.readOrDefault("response_format", String.class, "text")).thenReturn("text");
        when(context.readOrDefault("provider", String.class, "gemini")).thenReturn("gemini");
        when(context.readOrDefault("model", String.class, null)).thenReturn("gemini-1.5-pro");
        when(context.readOrDefault("model_options", Map.class, new HashMap<>())).thenReturn(new HashMap<>());
        when(context.readOrDefault("memory_key", String.class, null)).thenReturn(null);
        when(context.readOrDefault("include_history", Boolean.class, false)).thenReturn(false);
        when(context.readOrDefault("max_history_messages", Integer.class, 10)).thenReturn(10);

        // When: Execute cluster
        ExecutionResult result = executor.execute(context);

        // Then: Should succeed
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());

        // Verify provider was called (model variant passed via model_options)
        verify(orchestrator).executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        );
    }

    @Test
    void testMaterializedProviderNodePreferredWhenPresent() {
        setupBasicConfig("Summarize this text", "gemini", false, null);
        String parentKey = "ai_cluster_node";
        when(context.getNodeKey()).thenReturn(parentKey);

        BaseWorkflowNode childNode = new BaseWorkflowNode();
        childNode.setKey(parentKey + "::google-ai-gemini");
        childNode.setType(NodeType.PLUGIN);
        childNode.setPluginNode(new PluginNodeIdentifier(
                UUID.randomUUID(),
                "google-ai",
                "gemini",
                "1.0.0",
                "builtin"
        ));
        childNode.setConfig(new WorkflowConfig());

        when(context.getWorkflowNode(childNode.getKey())).thenReturn(childNode);
        when(orchestrator.executeNode(any(BaseWorkflowNode.class), any(WorkflowConfig.class), eq(context)))
                .thenReturn(ExecutionResult.success());

        ExecutionResult result = executor.execute(context);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        verify(orchestrator).executeNode(any(BaseWorkflowNode.class), any(WorkflowConfig.class), eq(context));
        verify(orchestrator, never()).executeSyntheticNodeByKey(
                eq("google-ai:gemini:1.0.0"),
                eq("ai_provider"),
                any(WorkflowConfig.class),
                eq(context)
        );
    }
}
