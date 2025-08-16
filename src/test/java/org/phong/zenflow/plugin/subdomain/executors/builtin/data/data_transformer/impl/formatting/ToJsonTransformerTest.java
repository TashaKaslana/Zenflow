package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.formatting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.formatting.ToJsonTransformer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ToJsonTransformerTest {

    private ToJsonTransformer toJsonTransformer;

    @BeforeEach
    void setUp() {
        toJsonTransformer = new ToJsonTransformer(new ObjectMapper());
    }

    @Test
    void testGetName() {
        assertEquals("to_json", toJsonTransformer.getName());
    }

    @Test
    void testTransformMap() {
        Map<String, Object> data = Map.of("name", "John", "age", 30, "active", true);
        Map<String, Object> params = new HashMap<>();

        String result = (String) toJsonTransformer.transform(data, params);

        assertNotNull(result);
        assertTrue(result.contains("\"name\""));
        assertTrue(result.contains("\"John\""));
        assertTrue(result.contains("\"age\""));
        assertTrue(result.contains("30"));
        assertTrue(result.contains("\"active\""));
        assertTrue(result.contains("true"));
    }

    @Test
    void testTransformList() {
        List<String> data = Arrays.asList("apple", "banana", "cherry");
        Map<String, Object> params = new HashMap<>();

        String result = (String) toJsonTransformer.transform(data, params);

        assertNotNull(result);
        assertTrue(result.contains("\"apple\""));
        assertTrue(result.contains("\"banana\""));
        assertTrue(result.contains("\"cherry\""));
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    void testTransformString() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) toJsonTransformer.transform("hello world", params);
        assertEquals("\"hello world\"", result);
    }

    @Test
    void testTransformNumber() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) toJsonTransformer.transform(123, params);
        assertEquals("123", result);
    }

    @Test
    void testTransformBoolean() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) toJsonTransformer.transform(true, params);
        assertEquals("true", result);
    }

    @Test
    void testTransformNull() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) toJsonTransformer.transform(null, params);
        assertEquals("null", result);
    }

    @Test
    void testTransformNestedStructure() {
        Map<String, Object> address = Map.of("street", "123 Main St", "city", "Boston");
        Map<String, Object> data = Map.of(
            "name", "John",
            "address", address,
            "hobbies", Arrays.asList("reading", "swimming")
        );
        Map<String, Object> params = new HashMap<>();

        String result = (String) toJsonTransformer.transform(data, params);

        assertNotNull(result);
        assertTrue(result.contains("\"address\""));
        assertTrue(result.contains("\"street\""));
        assertTrue(result.contains("\"123 Main St\""));
        assertTrue(result.contains("\"hobbies\""));
        assertTrue(result.contains("\"reading\""));
    }
}
