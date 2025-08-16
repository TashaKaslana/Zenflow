package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.aggregation.SortTransformer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SortTransformerTest {

    private SortTransformer sortTransformer;

    @BeforeEach
    void setUp() {
        sortTransformer = new SortTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("sort", sortTransformer.getName());
    }

    @Test
    void testSortBySingleFieldAscending() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Alice", "age", 25),
            Map.of("name", "Bob", "age", 35)
        );

        Map<String, Object> params = Map.of(
            "field", "age",
            "order", "asc",
            "type", "number"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) sortTransformer.transform(input, params);

        assertEquals(3, result.size());
        assertEquals("Alice", result.get(0).get("name"));
        assertEquals("John", result.get(1).get("name"));
        assertEquals("Bob", result.get(2).get("name"));
    }

    @Test
    void testSortBySingleFieldDescending() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Alice", "age", 25),
            Map.of("name", "Bob", "age", 35)
        );

        Map<String, Object> params = Map.of(
            "field", "age",
            "order", "desc",
            "type", "number"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) sortTransformer.transform(input, params);

        assertEquals(3, result.size());
        assertEquals("Bob", result.get(0).get("name"));
        assertEquals("John", result.get(1).get("name"));
        assertEquals("Alice", result.get(2).get("name"));
    }

    @Test
    void testSortByMultipleFields() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("department", "IT", "name", "John", "salary", 50000),
            Map.of("department", "IT", "name", "Alice", "salary", 60000),
            Map.of("department", "HR", "name", "Bob", "salary", 45000),
            Map.of("department", "IT", "name", "Charlie", "salary", 55000)
        );

        Map<String, Object> params = Map.of(
            "fields", Arrays.asList(
                Map.of("field", "department", "order", "asc", "type", "string"),
                Map.of("field", "salary", "order", "desc", "type", "number")
            )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) sortTransformer.transform(input, params);

        assertEquals(4, result.size());
        // HR department first
        assertEquals("HR", result.get(0).get("department"));
        assertEquals("Bob", result.get(0).get("name"));

        // IT department sorted by salary desc
        assertEquals("IT", result.get(1).get("department"));
        assertEquals("Alice", result.get(1).get("name")); // 60000
        assertEquals("IT", result.get(2).get("department"));
        assertEquals("Charlie", result.get(2).get("name")); // 55000
        assertEquals("IT", result.get(3).get("department"));
        assertEquals("John", result.get(3).get("name")); // 50000
    }

    @Test
    void testSortWithNullValues() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Alice"),
            Map.of("name", "Bob", "age", 25)
        );

        Map<String, Object> params = Map.of(
            "field", "age",
            "order", "asc",
            "type", "number",
            "nullsFirst", true
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) sortTransformer.transform(input, params);

        assertEquals(3, result.size());
        assertEquals("Alice", result.get(0).get("name")); // null age
        assertEquals("Bob", result.get(1).get("name"));   // age 25
        assertEquals("John", result.get(2).get("name"));  // age 30
    }

    @Test
    void testSortStringsCaseInsensitive() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "alice"),
            Map.of("name", "Bob"),
            Map.of("name", "CHARLIE")
        );

        Map<String, Object> params = Map.of(
            "field", "name",
            "order", "asc",
            "type", "string",
            "caseSensitive", false
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) sortTransformer.transform(input, params);

        assertEquals(3, result.size());
        assertEquals("alice", result.get(0).get("name"));
        assertEquals("Bob", result.get(1).get("name"));
        assertEquals("CHARLIE", result.get(2).get("name"));
    }

    @Test
    void testInvalidInputType() {
        assertThrows(DataTransformerExecutorException.class, () -> sortTransformer.transform("not a list", Map.of("field", "name")));
    }

    @Test
    void testMissingParameters() {
        List<Map<String, Object>> input = List.of(Map.of("name", "John"));

        assertThrows(DataTransformerExecutorException.class, () -> sortTransformer.transform(input, null));

        assertThrows(DataTransformerExecutorException.class, () -> sortTransformer.transform(input, Map.of()));
    }
}
