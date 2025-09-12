package org.phong.zenflow.plugin.subdomain.schema.services;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaMissingException;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.phong.zenflow.plugin.subdomain.schema.services.test.TestSchemaResource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaRegistry - UUID Integration Tests")
class SchemaRegistryUuidIntegrationTest {

    @Mock
    private PluginNodeSchemaProvider pluginProvider;

    @Mock
    private PluginService pluginService;

    private SchemaRegistry schemaRegistry;

    @Mock
    private SchemaIndexRegistry schemaIndexRegistry;

    private final String testNodeId1 = "123e4567-e89b-12d3-a456-426614174001";
    private final String testNodeId2 = "123e4567-e89b-12d3-a456-426614174002";
    private final String compositeKey1 = "email:send:1.0.0";

    @BeforeEach
    void setUp() {
        schemaRegistry = new SchemaRegistry(pluginProvider, pluginService, schemaIndexRegistry, 3600, true);

        // Mock schema index registry for UUID-based lookups
        lenient().when(schemaIndexRegistry.hasSchemaLocation(anyString())).thenReturn(true);
        lenient().when(schemaIndexRegistry.getSchemaLocation(anyString()))
                .thenReturn(new SchemaIndexRegistry.SchemaLocation(TestSchemaResource.class, "schema.json"));

        // Mock schema responses
        Map<String, Object> emailSchema = createMockSchema("email", "send");
        Map<String, Object> slackSchema = createMockSchema("slack", "message");

        // Use lenient stubbing since not all tests use all mock methods
        lenient().when(pluginProvider.getSchemaJson(anyString())).thenReturn(emailSchema); // Broad stubbing for database calls
    }

    @Test
    @DisplayName("Should retrieve schema by UUID template string")
    void shouldRetrieveSchemaByUuidTemplateString() {
        // Act
        JSONObject schema = schemaRegistry.getSchemaByTemplateString(testNodeId1);

        // Debugging: Print the schema to see its content
        System.out.println("Schema returned: " + schema.toString());

        // Assert
        assertNotNull(schema, "Schema should not be null");
        assertEquals("object", schema.getString("type"), "Schema type should be object");
        assertTrue(schema.has("properties"), "Schema should have properties");

        // Assert against the schema from TestSchemaResource.class
        JSONObject properties = schema.getJSONObject("properties");
        assertTrue(properties.has("testProperty"), "Schema should have testProperty");
        assertEquals("string", properties.getJSONObject("testProperty").getString("type"));

        // Verify that pluginProvider was NOT called for file-based loading
        verify(pluginProvider, never()).getSchemaJsonFromFile(testNodeId1);
        verify(pluginProvider, never()).getSchemaJson(testNodeId1);
    }

    @Test
    @DisplayName("Should retrieve schema by composite key template string as fallback")
    void shouldRetrieveSchemaByCompositeKeyTemplateString() {
        // Act
        JSONObject schema = schemaRegistry.getSchemaByTemplateString(compositeKey1);

        // Assert
        assertNotNull(schema, "Schema should not be null");
        assertEquals("object", schema.getString("type"), "Schema type should be object");
        assertTrue(schema.has("properties"), "Schema should have properties");

        // Assert against the schema from TestSchemaResource.class
        JSONObject properties = schema.getJSONObject("properties");
        assertTrue(properties.has("testProperty"), "Schema should have testProperty");
        assertEquals("string", properties.getJSONObject("testProperty").getString("type"));

        // Verify that pluginProvider was NOT called
        verify(pluginProvider, never()).getSchemaJsonFromFile(compositeKey1);
        verify(pluginProvider, never()).getSchemaJson(compositeKey1);
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

        // Verify all schemas are valid and assert against the schema from TestSchemaResource.class
        schemas.values().forEach(schema -> {
            assertNotNull(schema, "Each schema should not be null");
            assertEquals("object", schema.getString("type"), "Each schema type should be object");
            JSONObject properties = schema.getJSONObject("properties");
            assertTrue(properties.has("testProperty"), "Schema should have testProperty");
            assertEquals("string", properties.getJSONObject("testProperty").getString("type"));
        });

        // Verify that pluginProvider was NOT called
        verify(pluginProvider, never()).getAllSchemasByIdentifiersFromFile(anySet());
        verify(pluginProvider, never()).getAllSchemasByIdentifiers(anySet());
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

        // Verify that pluginProvider was NOT called
        verify(pluginProvider, never()).getSchemaJsonFromFile(testNodeId1);
        verify(pluginProvider, never()).getSchemaJson(testNodeId1);

        // Test cache invalidation
        schemaRegistry.invalidateByTemplateString(testNodeId1);

        // Act - Third access (should load from provider again after cache invalidation)
        JSONObject schema3 = schemaRegistry.getSchemaByTemplateString(testNodeId1);

        assertNotNull(schema3, "Schema retrieval after cache invalidation should succeed");

        // Verify that pluginProvider was NOT called again after cache invalidation
        verify(pluginProvider, never()).getSchemaJsonFromFile(testNodeId1);
        verify(pluginProvider, never()).getSchemaJson(testNodeId1);
    }

    @Test
    @DisplayName("Should handle database fallback when file-based loading is disabled")
    void shouldHandleDatabaseFallbackWhenFileBasedLoadingDisabled() {
        // Arrange - Create registry with file-based loading disabled
        SchemaRegistry dbRegistry = new SchemaRegistry(pluginProvider, pluginService, schemaIndexRegistry, 3600, false);

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
        String nonExistentUuid = "999e9999-e99b-92d3-a456-426614174001";

        // Mock schemaIndexRegistry to return a non-existent location
        lenient().when(schemaIndexRegistry.hasSchemaLocation(nonExistentUuid)).thenReturn(true);
        lenient().when(schemaIndexRegistry.getSchemaLocation(nonExistentUuid))
                .thenReturn(new SchemaIndexRegistry.SchemaLocation(TestSchemaResource.class, "non_existent_schema.json")); // Non-existent path

        // Crucial: Make pluginProvider.getSchemaJson return an empty map for the non-existent UUID
        lenient().when(pluginProvider.getSchemaJson(nonExistentUuid)).thenReturn(Map.of());

        // Act & Assert
        assertThrows(NodeSchemaMissingException.class, () -> schemaRegistry.getSchemaByTemplateString(nonExistentUuid), "Should throw NodeSchemaMissingException for non-existent UUID");

        // Verify that pluginProvider was NOT called for file-based loading
        verify(pluginProvider, never()).getSchemaJsonFromFile(nonExistentUuid);
        // Verify that pluginProvider.getSchemaJson was called
        verify(pluginProvider).getSchemaJson(nonExistentUuid);
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
        verify(pluginProvider, never()).getSchemaJsonFromFile(testNodeId1);
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
