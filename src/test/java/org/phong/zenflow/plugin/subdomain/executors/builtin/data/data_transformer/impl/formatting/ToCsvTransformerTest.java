package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.formatting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.formatting.ToCsvTransformer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ToCsvTransformerTest {

    private ToCsvTransformer toCsvTransformer;

    @BeforeEach
    void setUp() {
        toCsvTransformer = new ToCsvTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("to_csv", toCsvTransformer.getName());
    }

    @Test
    void testBasicCsvGeneration() {
        List<Map<String, Object>> input = Arrays.asList(
            createOrderedMap("name", "John", "age", 30, "city", "New York"),
            createOrderedMap("name", "Alice", "age", 25, "city", "London"),
            createOrderedMap("name", "Bob", "age", 35, "city", "Paris")
        );

        Map<String, Object> params = Map.of(
            "headers", true,
            "delimiter", ","
        );

        String result = (String) toCsvTransformer.transform(input, params);

        String[] lines = result.split("\n");
        assertEquals(4, lines.length);

        // Check headers (now predictable order)
        assertEquals("name,age,city", lines[0]);

        // Check data rows (now predictable order)
        assertEquals("John,30,New York", lines[1]);
        assertEquals("Alice,25,London", lines[2]);
        assertEquals("Bob,35,Paris", lines[3]);
    }

    // Helper method to create LinkedHashMap with predictable ordering
    private Map<String, Object> createOrderedMap(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    @Test
    void testCsvWithoutHeaders() {
        List<Map<String, Object>> input = Arrays.asList(
            createOrderedMap("name", "John", "age", 30),
            createOrderedMap("name", "Alice", "age", 25)
        );

        Map<String, Object> params = Map.of(
            "headers", false,
            "delimiter", ","
        );

        String result = (String) toCsvTransformer.transform(input, params);

        String[] lines = result.split("\n");
        assertEquals(2, lines.length);
        // Now we can check exact values since order is predictable
        assertEquals("John,30", lines[0]);
        assertEquals("Alice,25", lines[1]);
    }

    @Test
    void testCustomDelimiter() {
        List<Map<String, Object>> input = List.of(
                Map.of("name", "John", "age", 30)
        );

        Map<String, Object> params = Map.of(
            "headers", false,
            "delimiter", ";"
        );

        String result = (String) toCsvTransformer.transform(input, params);
        assertTrue(result.contains(";"));
        assertFalse(result.contains(","));
    }

    @Test
    void testSpecificColumns() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30, "email", "john@example.com"),
            Map.of("name", "Alice", "age", 25, "email", "alice@example.com")
        );

        Map<String, Object> params = Map.of(
            "headers", true,
            "columns", Arrays.asList("name", "age")
        );

        String result = (String) toCsvTransformer.transform(input, params);

        String[] lines = result.split("\n");
        assertEquals("name,age", lines[0]);
        assertEquals("John,30", lines[1]);
        assertEquals("Alice,25", lines[2]);
        assertFalse(result.contains("email"));
    }

    @Test
    void testIncludeIndex() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John"),
            Map.of("name", "Alice")
        );

        Map<String, Object> params = Map.of(
            "headers", true,
            "includeIndex", true
        );

        String result = (String) toCsvTransformer.transform(input, params);

        String[] lines = result.split("\n");
        assertTrue(lines[0].startsWith("index,") || lines[0].endsWith(",index"));
        assertTrue(lines[1].startsWith("0,") || lines[1].endsWith(",0"));
        assertTrue(lines[2].startsWith("1,") || lines[2].endsWith(",1"));
    }

    @Test
    void testValuesWithCommas() {
        List<Map<String, Object>> input = List.of(
                Map.of("name", "John, Jr.", "description", "A person with, comma")
        );

        Map<String, Object> params = Map.of(
            "headers", false,
            "quote", "\""
        );

        String result = (String) toCsvTransformer.transform(input, params);
        assertTrue(result.contains("\"John, Jr.\""));
        assertTrue(result.contains("\"A person with, comma\""));
    }

    @Test
    void testEmptyList() {
        List<Map<String, Object>> input = Collections.emptyList();

        String result = (String) toCsvTransformer.transform(input, null);
        assertEquals("", result);
    }

    @Test
    void testPrimitiveValues() {
        List<String> input = Arrays.asList("John", "Alice", "Bob");

        Map<String, Object> params = Map.of(
            "headers", true
        );

        String result = (String) toCsvTransformer.transform(input, params);

        String[] lines = result.split("\n");
        assertEquals(4, lines.length);
        assertEquals("value", lines[0]);
        assertEquals("John", lines[1]);
        assertEquals("Alice", lines[2]);
        assertEquals("Bob", lines[3]);
    }

    @Test
    void testInvalidInputType() {
        assertThrows(DataTransformerExecutorException.class, () -> toCsvTransformer.transform("not a list", null));
    }
}
