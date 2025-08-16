package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.aggregation.DistinctTransformer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DistinctTransformerTest {

    private DistinctTransformer distinctTransformer;

    @BeforeEach
    void setUp() {
        distinctTransformer = new DistinctTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("distinct", distinctTransformer.getName());
    }

    @Test
    void testTransformWithDuplicateStrings() {
        List<String> input = Arrays.asList("apple", "banana", "apple", "cherry", "banana", "apple");
        Map<String, Object> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) distinctTransformer.transform(input, params);

        assertEquals(3, result.size());
        assertTrue(result.contains("apple"));
        assertTrue(result.contains("banana"));
        assertTrue(result.contains("cherry"));
    }

    @Test
    void testTransformWithNoDuplicates() {
        List<String> input = Arrays.asList("apple", "banana", "cherry");
        Map<String, Object> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) distinctTransformer.transform(input, params);

        assertEquals(3, result.size());
        assertEquals(input, result);
    }

    @Test
    void testTransformWithNumbers() {
        List<Integer> input = Arrays.asList(1, 2, 3, 2, 4, 1, 5);
        Map<String, Object> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) distinctTransformer.transform(input, params);

        assertEquals(5, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
        assertTrue(result.contains(4));
        assertTrue(result.contains(5));
    }

    @Test
    void testTransformWithMaps() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("id", 1, "name", "John"),
            Map.of("id", 2, "name", "Jane"),
            Map.of("id", 1, "name", "John"),
            Map.of("id", 3, "name", "Bob")
        );
        Map<String, Object> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) distinctTransformer.transform(input, params);

        assertEquals(3, result.size());
    }

    @Test
    void testTransformWithEmptyList() {
        List<String> input = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) distinctTransformer.transform(input, params);

        assertTrue(result.isEmpty());
    }

    @Test
    void testTransformWithNullValues() {
        List<String> input = Arrays.asList("apple", null, "banana", null, "cherry");
        Map<String, Object> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) distinctTransformer.transform(input, params);

        assertEquals(4, result.size());
        assertTrue(result.contains("apple"));
        assertTrue(result.contains("banana"));
        assertTrue(result.contains("cherry"));
        assertTrue(result.contains(null));
    }

    @Test
    void testTransformWithNonListInput() {
        Map<String, Object> params = new HashMap<>();

        assertThrows(DataTransformerExecutorException.class, () -> distinctTransformer.transform("not a list", params));
    }

    @Test
    void testTransformWithNullInput() {
        Map<String, Object> params = new HashMap<>();

        assertThrows(DataTransformerExecutorException.class, () -> distinctTransformer.transform(null, params));
    }

    @Test
    void testTransformPreservesOrder() {
        List<String> input = Arrays.asList("zebra", "apple", "banana", "apple", "cherry", "zebra");
        Map<String, Object> params = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) distinctTransformer.transform(input, params);

        assertEquals(4, result.size());
        // Should preserve the order of first occurrence
        assertEquals("zebra", result.get(0));
        assertEquals("apple", result.get(1));
        assertEquals("banana", result.get(2));
        assertEquals("cherry", result.get(3));
    }
}
