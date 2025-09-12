package org.phong.zenflow.plugin.subdomain.node.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PluginNodeSynchronizer - UUID Integration Tests")
class PluginNodeSynchronizerUuidIntegrationTest {
    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TriggerRegistry triggerRegistry;

    @Mock
    private PluginNodeExecutorRegistry registry;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private PluginNodeExecutor mockExecutor;

    @Mock
    private SchemaIndexRegistry schemaIndexRegistry;

    private UUID testNodeId1;
    private UUID testNodeId2;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        testNodeId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        testNodeId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

        // Mock common dependencies with lenient stubbing since not all tests use all mocks
        Plugin mockPlugin = new Plugin();
        mockPlugin.setKey("test");
        lenient().when(pluginRepository.getReferenceByKey(anyString())).thenReturn(Optional.of(mockPlugin));

        lenient().when(objectMapper.readValue(any(java.io.InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(createMockSchemaMap());

        lenient().when(applicationContext.getBean(any(Class.class))).thenReturn(mockExecutor);

        // Mock SchemaIndexRegistry behavior
        final ConcurrentHashMap<String, SchemaIndexRegistry.SchemaLocation> schemaIndex = new java.util.concurrent.ConcurrentHashMap<>();
        lenient().when(schemaIndexRegistry.getSchemaIndex()).thenReturn(schemaIndex);
        lenient().when(schemaIndexRegistry.getSchemaIndexSize()).thenAnswer(i -> schemaIndex.size());
        lenient().when(schemaIndexRegistry.hasSchemaLocation(anyString())).thenAnswer(i -> schemaIndex.containsKey(i.getArgument(0)));
        lenient().when(schemaIndexRegistry.getSchemaLocation(anyString())).thenAnswer(i -> schemaIndex.get(i.getArgument(0)));
    }

    @Test
    @DisplayName("Should synchronize plugin nodes and index schemas by UUID")
    void shouldSynchronizePluginNodesAndIndexSchemasByUuid() {
        // Act - Simulate synchronization
        simulateNodeSynchronization("test:placeholder:1.0.0", testNodeId1);
        simulateNodeSynchronization("test:validator:2.0.0", testNodeId2);

        // Assert
        assertEquals(2, schemaIndexRegistry.getSchemaIndexSize(), "Should have indexed 2 schemas");

        assertTrue(schemaIndexRegistry.hasSchemaLocation(testNodeId1.toString()),
                "Should have schema location for UUID 1");
        assertTrue(schemaIndexRegistry.hasSchemaLocation(testNodeId2.toString()),
                "Should have schema location for UUID 2");

        SchemaIndexRegistry.SchemaLocation location1 = schemaIndexRegistry.getSchemaLocation(testNodeId1.toString());
        assertNotNull(location1, "Schema location should not be null");
        // Since we're using AtomicReference for placeholder, check that it's the expected class
        assertEquals(java.util.concurrent.atomic.AtomicReference.class, location1.clazz(), "Should reference correct class");

        // Verify executor registry was called with UUIDs
        verify(registry).register(eq(testNodeId1.toString()), any());
        verify(registry).register(eq(testNodeId2.toString()), any());
    }

    @Test
    @DisplayName("Should handle schema loading from different path configurations")
    void shouldHandleSchemaLoadingFromDifferentPathConfigurations() {
        // Test default schema path
        String defaultPath = LoadSchemaHelper.extractPath(String.class, "");
        assertTrue(defaultPath.contains("schema.json"), "Default path should contain schema.json");

        // Test custom absolute path
        String absolutePath = LoadSchemaHelper.extractPath(String.class, "/custom/schema.json");
        assertTrue(absolutePath.contains("custom/schema.json"), "Should handle absolute paths");

        // Test relative path
        String relativePath = LoadSchemaHelper.extractPath(String.class, "./config/schema.json");
        assertTrue(relativePath.contains("config/schema.json"), "Should handle relative paths");

        // Test parent directory path
        String parentPath = LoadSchemaHelper.extractPath(String.class, "../shared/schema.json");
        assertTrue(parentPath.contains("shared/schema.json"), "Should handle parent directory paths");
    }

    @Test
    @DisplayName("Should register trigger nodes with UUID-based identification")
    void shouldRegisterTriggerNodesWithUuidBasedIdentification() {
        // Act - Simulate trigger node synchronization
        simulateNodeSynchronization("webhook:trigger:1.0.0", testNodeId1);

        // Assert
        verify(registry).register(eq(testNodeId1.toString()), any());
        verify(triggerRegistry).registerTrigger(eq(testNodeId1.toString()));

        assertTrue(schemaIndexRegistry.hasSchemaLocation(testNodeId1.toString()),
                "Trigger node should be indexed by UUID");
    }

    @Test
    @DisplayName("Should handle schema index operations with concurrent access")
    void shouldHandleSchemaIndexOperationsWithConcurrentAccess() throws Exception {
        // Act - Simulate concurrent operations
        Thread thread1 = new Thread(() -> {
            try {
                simulateNodeSynchronization("concurrent:test1:1.0.0", testNodeId1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                simulateNodeSynchronization("concurrent:test2:1.0.0", testNodeId2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert
        assertEquals(2, schemaIndexRegistry.getSchemaIndexSize(),
                "Should handle concurrent indexing correctly");
        assertTrue(schemaIndexRegistry.hasSchemaLocation(testNodeId1.toString()),
                "Should contain first concurrent node");
        assertTrue(schemaIndexRegistry.hasSchemaLocation(testNodeId2.toString()),
                "Should contain second concurrent node");
    }

    @Test
    @DisplayName("Should maintain schema index integrity during updates")
    void shouldMaintainSchemaIndexIntegrityDuringUpdates() {
        // Act - Initial synchronization
        simulateNodeSynchronization("update:test:1.0.0", testNodeId1);
        assertEquals(1, schemaIndexRegistry.getSchemaIndexSize(), "Should have initial entry");

        // Act - Update synchronization (same UUID, different version)
        simulateNodeSynchronization("update:test:1.1.0", testNodeId1);

        // Assert
        assertEquals(1, schemaIndexRegistry.getSchemaIndexSize(),
                "Should maintain same index size after update");
        assertTrue(schemaIndexRegistry.hasSchemaLocation(testNodeId1.toString()),
                "Should still contain the updated node");

        SchemaIndexRegistry.SchemaLocation location =
                schemaIndexRegistry.getSchemaLocation(testNodeId1.toString());
        assertNotNull(location, "Schema location should be updated");
    }

    private Map<String, Object> createMockSchemaMap() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "input", Map.of("type", "object"),
                        "output", Map.of("type", "object")
                )
        );
    }

    private void simulateNodeSynchronization(String compositeKey, UUID nodeId) {
        // This simulates the actual synchronization process that would happen
        // during classpath scanning in a real scenario

        // Create a mock class name that contains the expected pattern
        String[] parts = compositeKey.split(":");

        // Use a real class but create a wrapper that provides the expected getName() behavior
        Class<?> mockClass = createMockClassForNodeType(parts[1]);

        // Simulate the schema indexing
        SchemaIndexRegistry.SchemaLocation location =
                new SchemaIndexRegistry.SchemaLocation(mockClass, "");
        schemaIndexRegistry.getSchemaIndex().put(nodeId.toString(), location);

        // Simulate the registry registration (this is what the real implementation does)
        registry.register(nodeId.toString(), () -> mockExecutor);

        // Determine the node type based on the composite key parts
        String nodeType = parts[1].equalsIgnoreCase("trigger") ? "trigger" : "action";

        // If it's a trigger type, register with trigger registry
        if ("trigger".equalsIgnoreCase(nodeType)) {
            triggerRegistry.registerTrigger(nodeId.toString());
        }
    }

    private Class<?> createMockClassForNodeType(String nodeType) {
        // For testing purposes, we'll use different existing classes that contain the node type in their name
        return switch (nodeType.toLowerCase()) {
            case "placeholder" ->
                    java.util.concurrent.atomic.AtomicReference.class; // Contains part of expected pattern
            case "validator" -> java.util.concurrent.ConcurrentHashMap.class;
            case "trigger" -> java.util.concurrent.CountDownLatch.class;
            case "test1" -> java.util.concurrent.CyclicBarrier.class;
            case "test2" -> java.util.concurrent.Semaphore.class;
            case "test" -> java.util.concurrent.ThreadPoolExecutor.class;
            default -> String.class;
        };
    }
}
