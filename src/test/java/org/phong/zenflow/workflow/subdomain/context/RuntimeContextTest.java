package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RuntimeContextTest {

    private RuntimeContext context;

    @BeforeEach
    void setUp() {
        context = new RuntimeContext();
    }

    @Test
    void putStoresHierarchicalValues() {
        context.put("node.output.value", 42);

        assertEquals(42, context.get("node.output.value"));
        Object nodeConfig = context.get("node.output");
        assertNotNull(nodeConfig);
        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) nodeConfig;
        assertEquals(42, output.get("value"));
    }

    @Test
    void initializeMigratesFlatEntries() {
        context.initialize(Map.of("node.output.value", "hello"), null, null);

        assertEquals("hello", context.get("node.output.value"));
    }

    @Test
    void getAndCleanRemovesValuesWithoutConsumers() {
        context.put("node.config.input.foo", "bar");

        assertEquals("bar", context.getAndClean("node", "node.config.input.foo"));
        assertEquals(null, context.getAndClean("node", "node.config.input.foo"));
    }
}
