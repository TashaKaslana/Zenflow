package org.phong.zenflow.plugin.subdomain.schema.services;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaMissingException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaRegistry - UUID Integration Tests")
class SchemaRegistryUuidIntegrationTest {

    @Mock
    private PluginNodeSchemaProvider pluginProvider;

    private SchemaRegistry schemaRegistry;

    private final String testNodeId1 = "123e4567-e89b-12d3-a456-426614174001";
    private final String testNodeId2 = "123e4567-e89b-12d3-a456-426614174002";
    private final String compositeKey1 = "email:send:1.0.0";

    @BeforeEach
    void setUp() {
        schemaRegistry = new SchemaRegistry(pluginProvider, 3600, true);

        // Mock schema responses
        Map<String, Object> emailSchema = createMockSchema("email", "send");
        Map<String, Object> slackSchema = createMockSchema("slack", "message");

        // Use lenient stubbing since not all tests use all mock methods
        lenient().when(pluginProvider.getSchemaJsonFromFile(testNodeId1)).thenReturn(emailSchema);
        lenient().when(pluginProvider.getSchemaJsonFromFile(testNodeId2)).thenReturn(slackSchema);
        lenient().when(pluginProvider.getSchemaJson(testNodeId1)).thenReturn(emailSchema);
        lenient().when(pluginProvider.getSchemaJson(testNodeId2)).thenReturn(slackSchema);
        lenient().when(pluginProvider.getSchemaJsonFromFile(compositeKey1)).thenReturn(emailSchema);

        lenient().when(pluginProvider.getAllSchemasByIdentifiersFromFile(anySet())).thenAnswer(invocation -> {
            Set<String> ids = invocation.getArgument(0);
            Map<String, Map<String, Object>> result = new HashMap<>();
            for (String id : ids) {
                if (testNodeId1.equals(id)) {
                    result.put(id, emailSchema);
                } else if (testNodeId2.equals(id)) {
                    result.put(id, slackSchema);
                } else if (compositeKey1.equals(id)) {
                    result.put(id, emailSchema);
                }
            }
            return result;
        });

        lenient().when(pluginProvider.getAllSchemasByIdentifiers(anySet())).thenAnswer(invocation -> {
            Set<String> ids = invocation.getArgument(0);
            Map<String, Map<String, Object>> result = new HashMap<>();
            for (String id : ids) {
                if (testNodeId1.equals(id)) {
                    result.put(id, emailSchema);
                } else if (testNodeId2.equals(id)) {
                    result.put(id, slackSchema);
                } else if (compositeKey1.equals(id)) {
                    result.put(id, emailSchema);
                }
            }
            return result;
        });
    }

    @Test
    @DisplayName("Should retrieve schema by UUID template string")
    void shouldRetrieveSchemaByUuidTemplateString() {
        // Act
        JSONObject schema = schemaRegistry.getSchemaByTemplateString(testNodeId1);

        // Assert
        assertNotNull(schema, "Schema should not be null");
        assertEquals("object", schema.getString("type"), "Schema type should be object");
        assertTrue(schema.has("properties"), "Schema should have properties");

        JSONObject properties = schema.getJSONObject("properties");
        assertEquals("email", properties.getJSONObject("pluginType").getString("default"));
        assertEquals("send", properties.getJSONObject("nodeType").getString("default"));

        // Verify UUID was used for file-based loading (due to useFileBasedLoading = true)
        verify(pluginProvider).getSchemaJsonFromFile(testNodeId1);
        verify(pluginProvider, never()).getSchemaJson(testNodeId1);
    }

    @Test
    @DisplayName("Should retrieve schema by composite key template string as fallback")
    void shouldRetrieveSchemaByCompositeKeyTemplateString() {
        // Mock composite key fallback
        Map<String, Object> compositeSchema = createMockSchema("composite", "fallback");
        when(pluginProvider.getSchemaJsonFromFile(compositeKey1)).thenReturn(compositeSchema);

        // Act
        JSONObject schema = schemaRegistry.getSchemaByTemplateString(compositeKey1);

        // Assert
        assertNotNull(schema, "Schema should not be null");
        assertEquals("object", schema.getString("type"), "Schema type should be object");
        assertTrue(schema.has("properties"), "Schema should have properties");

        // Verify composite key was used
        verify(pluginProvider).getSchemaJsonFromFile(compositeKey1);
    }

    @Test
    @DisplayName("Should handle batch schema loading with mixed UUID and composite keys")
    void shouldHandleBatchSchemaLoadingWithMixedKeys() {
        // Arrange - Test only UUID and composite key loading, skip builtin schema
        Set<String> templateStrings = Set.of(
                testNodeId1,             // UUID template string
                testNodeId2,             // UUID template string
                compositeKey1            // Composite key template string
        );

        // Act
        Map<String, JSONObject> schemas = schemaRegistry.getSchemaMapByTemplateStrings(templateStrings);

        // Assert
        assertEquals(3, schemas.size(), "Should return all 3 schemas");

        assertTrue(schemas.containsKey(testNodeId1), "Should contain UUID schema 1");
        assertTrue(schemas.containsKey(testNodeId2), "Should contain UUID schema 2");
        assertTrue(schemas.containsKey(compositeKey1), "Should contain composite key schema");

        // Verify all schemas are valid
        schemas.values().forEach(schema -> {
            assertNotNull(schema, "Each schema should not be null");
            assertEquals("object", schema.getString("type"), "Each schema type should be object");
        });

        // Verify batch loading was used for UUID schemas
        verify(pluginProvider).getAllSchemasByIdentifiersFromFile(Set.of(testNodeId1, testNodeId2, compositeKey1));
    }

    @Test
    @DisplayName("Should handle schema caching with UUID-based keys")
    void shouldHandleSchemaCachingWithUuidKeys() {
        // Act - First access (should load from provider)
        JSONObject schema1 = schemaRegistry.getSchemaByTemplateString(testNodeId1);

        // Act - Second access (should use cache)
        JSONObject schema2 = schemaRegistry.getSchemaByTemplateString(testNodeId1);

        // Assert
        assertNotNull(schema1, "First schema retrieval should succeed");
        assertNotNull(schema2, "Second schema retrieval should succeed");
        assertEquals(schema1.getString("type"), schema2.getString("type"), "Cached schema should match original");

        // Verify provider was called only once due to caching
        verify(pluginProvider, times(1)).getSchemaJsonFromFile(testNodeId1);

        // Test cache invalidation
        schemaRegistry.invalidateByTemplateString(testNodeId1);

        // Act - Third access (should load from provider again after cache invalidation)
        JSONObject schema3 = schemaRegistry.getSchemaByTemplateString(testNodeId1);

        assertNotNull(schema3, "Schema retrieval after cache invalidation should succeed");

        // Verify provider was called again after cache invalidation
        verify(pluginProvider, times(2)).getSchemaJsonFromFile(testNodeId1);
    }

    @Test
    @DisplayName("Should handle database fallback when file-based loading is disabled")
    void shouldHandleDatabaseFallbackWhenFileBasedLoadingDisabled() {
        // Arrange - Create registry with file-based loading disabled
        SchemaRegistry dbRegistry = new SchemaRegistry(pluginProvider, 3600, false);

        // Act
        JSONObject schema = dbRegistry.getSchemaByTemplateString(testNodeId1);

        // Assert
        assertNotNull(schema, "Schema should be loaded from database");
        assertEquals("object", schema.getString("type"), "Database schema should be valid");

        // Verify database was used instead of file-based loading
        verify(pluginProvider).getSchemaJson(testNodeId1);
        verify(pluginProvider, never()).getSchemaJsonFromFile(testNodeId1);
    }

    @Test
    @DisplayName("Should throw exception when schema not found for UUID")
    void shouldThrowExceptionWhenSchemaNotFoundForUuid() {
        // Arrange
        String nonExistentUuid = "999e9999-e99b-99d9-a999-999999999999";
        when(pluginProvider.getSchemaJsonFromFile(nonExistentUuid)).thenReturn(Map.of());

        // Act & Assert
        assertThrows(NodeSchemaMissingException.class, () -> schemaRegistry.getSchemaByTemplateString(nonExistentUuid), "Should throw NodeSchemaMissingException for non-existent UUID");

        verify(pluginProvider).getSchemaJsonFromFile(nonExistentUuid);
    }

    @Test
    @DisplayName("Should handle force database and file loading methods with UUIDs")
    void shouldHandleForceLoadingMethodsWithUuids() {
        // Act & Assert - Force database loading
        assertDoesNotThrow(() -> {
            JSONObject dbSchema = schemaRegistry.getPluginSchemaFromDatabase(testNodeId1);
            assertNotNull(dbSchema, "Database schema should not be null");
            assertEquals("object", dbSchema.getString("type"), "Database schema should be valid");
        });

        // Act & Assert - Force file loading
        assertDoesNotThrow(() -> {
            JSONObject fileSchema = schemaRegistry.getPluginSchemaFromFile(testNodeId1);
            assertNotNull(fileSchema, "File schema should not be null");
            assertEquals("object", fileSchema.getString("type"), "File schema should be valid");
        });

        // Verify both methods were called with UUID
        verify(pluginProvider).getSchemaJson(testNodeId1);
        verify(pluginProvider, atLeast(1)).getSchemaJsonFromFile(testNodeId1);
    }

    // Helper method
    private Map<String, Object> createMockSchema(String pluginType, String nodeType) {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "input", Map.of("type", "object", "properties", Map.of()),
                        "output", Map.of("type", "object", "properties", Map.of()),
                        "pluginType", Map.of("type", "string", "default", pluginType),
                        "nodeType", Map.of("type", "string", "default", nodeType)
                ),
                "required", List.of("input", "output")
        );
    }
}
