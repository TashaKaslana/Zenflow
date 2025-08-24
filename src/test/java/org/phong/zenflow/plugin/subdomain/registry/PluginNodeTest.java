package org.phong.zenflow.plugin.subdomain.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.register.ExecutorInitializer;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNodeSchemaIndex;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.placeholder.PlaceholderExecutor;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeSchemaProviderImpl;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNodeSynchronizer;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.TestExecutionContextUtils;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {
        PlaceholderExecutor.class,
        PluginNodeExecutorRegistry.class,
        ExecutorInitializer.class,
        SchemaRegistry.class,
        PluginNodeExecutorDispatcher.class,
        PluginNodeSchemaProviderImpl.class,
        PluginNodeSynchronizer.class,
        PluginNodeSchemaIndex.class,
        ObjectMapper.class
})
public class PluginNodeTest {

    @MockitoBean
    private PluginNodeRepository pluginNodeRepository;

    @MockitoBean
    private PluginRepository pluginRepository;

    @Autowired
    private PluginNodeExecutorRegistry executorRegistry;

    @Autowired
    private SchemaRegistry schemaRegistry;

    @Autowired
    private PluginNodeSchemaProvider schemaProvider;

    @Autowired
    private PluginNodeExecutorDispatcher dispatcher;

    @Test
    void isPluginRegistered() {
        // Test that the placeholder plugin node is properly registered
        PluginNodeIdentifier placeholderIdentifier = PluginNodeIdentifier.fromString("test:placeholder:1.0.0", "builtin");

        assertTrue(
                executorRegistry.getExecutor(placeholderIdentifier).isPresent(),
                "Placeholder executor should be registered in the registry"
        );

        var executor = executorRegistry.getExecutor(placeholderIdentifier).orElseThrow();
        assertEquals(
                "test:placeholder:1.0.0",
                executor.key(),
                "Executor key should match the expected format"
        );
    }

    @Test
    void testSchemaLoading() {
        // Test that the placeholder node's schema can be loaded successfully
        String templateString = "test:placeholder:1.0.0";

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
        // Test direct file-based schema loading for performance
        PluginNodeIdentifier identifier = PluginNodeIdentifier.fromString("test:placeholder:1.0.0", "builtin");

        assertDoesNotThrow(() -> {
            // Test file-based loading directly through SchemaRegistry
            JSONObject schemaFromFile = schemaRegistry.getPluginSchemaFromFile(identifier);
            assertNotNull(schemaFromFile, "File-based schema should not be null");

            // Verify schema structure from file
            assertTrue(schemaFromFile.has("properties"), "File schema should have properties");
            assertTrue(schemaFromFile.has("required"), "File schema should have required fields");
            assertEquals("object", schemaFromFile.getString("type"), "File schema type should be object");

            // Test direct provider access
            Map<String, Object> schemaMap = schemaProvider.getSchemaJsonFromFile(identifier);
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
        // Test that the dispatcher can correctly dispatch to the right executor
        PluginNodeIdentifier placeholderIdentifier = PluginNodeIdentifier.fromString("test:placeholder:1.0.0", "builtin");

        // Create test configuration
        Map<String, Object> inputData = Map.of(
                "testKey1", "testValue1",
                "testKey2", 123,
                "testKey3", true
        );
        WorkflowConfig config = new WorkflowConfig(inputData);
        ExecutionContext context = TestExecutionContextUtils.createExecutionContext();

        // Test dispatch execution
        ExecutionResult result = dispatcher.dispatch(placeholderIdentifier, config, context);

        assertNotNull(result, "Execution result should not be null");
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus(), "Execution should be successful");

        // Verify the output matches the input (placeholder behavior)
        Map<String, Object> output = result.getOutput();
        assertNotNull(output, "Output should not be null");
        assertEquals("testValue1", output.get("testKey1"), "Output should contain testKey1 with correct value");
        assertEquals(123, output.get("testKey2"), "Output should contain testKey2 with correct value");
        assertEquals(true, output.get("testKey3"), "Output should contain testKey3 with correct value");

        // Logging is now handled asynchronously via NodeLogPublisher
        // ExecutionResult no longer carries node logs directly
        assertNull(result.getLogs(), "Logs should be managed by the logging subsystem");
    }
}
