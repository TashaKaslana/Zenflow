package org.phong.zenflow.plugin.subdomain.registry;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.execution.services.NodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder.PlaceholderExecutor;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.plugin.subdomain.schema.services.PluginDescriptorSchemaService;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class PluginNodeTest {

    @Mock
    private PluginNodeSchemaProvider schemaProvider;

    @Mock
    private PluginService pluginService;

    private PluginNodeExecutorRegistry executorRegistry;
    private SchemaRegistry schemaRegistry;
    private NodeExecutorDispatcher dispatcher;
    private PlaceholderExecutor placeholderExecutor;
    @Mock
    private SchemaIndexRegistry schemaIndexRegistry;

    private final String placeholderUuid = "123e4567-e89b-12d3-a456-426614174001";
    private final String placeholderCompositeKey = "test:placeholder:1.0.0";

    @BeforeEach
    void setUp() {
        // Initialize test components
        executorRegistry = new PluginNodeExecutorRegistry();
        placeholderExecutor = new PlaceholderExecutor();
        PluginDescriptorSchemaService descriptorService = new PluginDescriptorSchemaService(pluginService, schemaIndexRegistry, 3600L, true);
        schemaRegistry = new SchemaRegistry(schemaProvider, pluginService, schemaIndexRegistry, descriptorService, 3600L, true);
        dispatcher = new NodeExecutorDispatcher(executorRegistry);

        // Manually register the placeholder executor for testing
        NodeDefinition placeholderDefinition = NodeDefinition.builder()
                .nodeExecutor(placeholderExecutor)
                .build();
        executorRegistry.register(placeholderUuid, () -> placeholderDefinition);
        executorRegistry.register(placeholderCompositeKey, () -> placeholderDefinition);

        // Mock schema index registry for UUID-based lookups
        lenient().when(schemaIndexRegistry.hasSchemaLocation(anyString())).thenReturn(true);
        lenient().when(schemaIndexRegistry.getSchemaLocation(anyString()))
                .thenReturn(new SchemaIndexRegistry.SchemaLocation(PlaceholderExecutor.class, "schema.json"));
    }

    private void setupSchemaProviderMock() {
        // Mock the schema provider to return a test schema - only when needed
        Map<String, Object> testSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "input", Map.of("type", "object"),
                        "output", Map.of("type", "object")
                ),
                "required", new String[]{"input"}
        );

        lenient().when(schemaProvider.getSchemaJsonFromFile(anyString())).thenReturn(testSchema);
        lenient().when(schemaProvider.getSchemaJson(anyString())).thenReturn(testSchema);
    }

    @Test
    void isPluginRegistered() {
        // Test that the placeholder plugin node is properly registered
        // Use UUID string for registry lookup (new UUID-based approach)
        assertTrue(
                executorRegistry.getDefinition(placeholderUuid).isPresent(),
                "Placeholder executor should be registered in the registry with UUID"
        );

        var executor = executorRegistry.getDefinition(placeholderUuid).orElseThrow();
        assertInstanceOf(PlaceholderExecutor.class, executor.getNodeExecutor(), "Executor should be a PlaceholderExecutor instance");

        // Test fallback to composite key if UUID not available
        assertTrue(
                executorRegistry.getDefinition(placeholderCompositeKey).isPresent(),
                "Placeholder executor should also be registered with composite key as fallback"
        );
    }

    @Test
    void testSchemaLoading() {
        setupSchemaProviderMock(); // Only set up mocks when needed

        // Test that the placeholder node's schema can be loaded successfully
        // Use UUID string as template string (new approach)
        String templateString = placeholderUuid;

        assertDoesNotThrow(() -> {
            JSONObject schema = schemaRegistry.getSchemaByTemplateString(templateString);
            assertNotNull(schema, "Schema should not be null");

            // Verify the schema contains expected properties
            assertTrue(schema.has("properties"), "Schema should have properties section");
            assertTrue(schema.has("required"), "Schema should have required section");
            assertTrue(schema.has("type"), "Schema should have type field");

            JSONObject properties = schema.getJSONObject("properties");
            assertTrue(properties.has("input"), "Schema should define input property");
            assertTrue(properties.has("output"), "Schema should define output property");

            // Verify schema type
            assertEquals("object", schema.getString("type"), "Schema type should be object");
        }, "Schema loading should not throw any exceptions");
    }

    @Test
    void testFileBasedSchemaLoading() {
        setupSchemaProviderMock(); // Only set up mocks when needed

        // Test direct file-based schema loading for performance using UUID strings
        assertDoesNotThrow(() -> {
            // Test file-based loading directly through SchemaRegistry with UUID
            JSONObject schemaFromFile = schemaRegistry.getPluginSchemaFromFile(placeholderUuid);
            assertNotNull(schemaFromFile, "File-based schema should not be null");

            // Verify schema structure from file
            assertTrue(schemaFromFile.has("properties"), "File schema should have properties");
            assertTrue(schemaFromFile.has("required"), "File schema should have required fields");
            assertEquals("object", schemaFromFile.getString("type"), "File schema type should be object");

            // Test direct provider access with UUID
            Map<String, Object> schemaMap = schemaProvider.getSchemaJsonFromFile(placeholderUuid);
            assertNotNull(schemaMap, "Provider file schema should not be null");
            assertFalse(schemaMap.isEmpty(), "Provider file schema should not be empty");

            // Verify consistency between SchemaRegistry and Provider
            JSONObject providerSchema = new JSONObject(schemaMap);
            assertEquals(schemaFromFile.getString("type"), providerSchema.getString("type"),
                "Schema type should be consistent between registry and provider");

        }, "File-based schema loading should not throw any exceptions");
    }

    @Test
    void testExecutorDispatch() {
        // Test that the dispatcher can correctly dispatch to the right executor using UUID
        // Create test configuration
        Map<String, Object> inputData = Map.of(
                "testKey1", "testValue1",
                "testKey2", 123,
                "testKey3", true
        );
        WorkflowConfig config = new WorkflowConfig(inputData, Map.of());
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();

        // Test dispatch execution with UUID and correct executor type "builtin"
        ExecutionResult result = dispatcher.dispatch(placeholderUuid, "builtin", config, context);

        assertNotNull(result, "Execution result should not be null");
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus(), "Execution should be successful");

        // Verify the output contains the expected data
        assertNotNull(result.getOutput(), "Output should not be null");
        assertTrue(result.getOutput().containsKey("testKey1"), "Output should contain testKey1");
        assertEquals("testValue1", result.getOutput().get("testKey1"), "Output should match input for testKey1");
    }

    @Test
    void testUuidAndCompositeKeyCompatibility() {
        setupSchemaProviderMock(); // Only set up mocks when needed

        // Test that both UUID and composite key approaches work

        // Test UUID-based access
        assertTrue(
                executorRegistry.getDefinition(placeholderUuid).isPresent(),
                "Should work with UUID"
        );

        // Test composite key fallback
        assertTrue(
                executorRegistry.getDefinition(placeholderCompositeKey).isPresent(),
                "Should work with composite key as fallback"
        );

        // Test schema access with both approaches
        assertDoesNotThrow(() -> {
            JSONObject uuidSchema = schemaRegistry.getSchemaByTemplateString(placeholderUuid);
            JSONObject compositeSchema = schemaRegistry.getSchemaByTemplateString(placeholderCompositeKey);

            assertNotNull(uuidSchema, "UUID-based schema should not be null");
            assertNotNull(compositeSchema, "Composite key schema should not be null");

            // Both should return valid schemas
            assertEquals("object", uuidSchema.getString("type"), "UUID schema should be valid");
            assertEquals("object", compositeSchema.getString("type"), "Composite schema should be valid");
        }, "Both UUID and composite key schema access should work");
    }
}