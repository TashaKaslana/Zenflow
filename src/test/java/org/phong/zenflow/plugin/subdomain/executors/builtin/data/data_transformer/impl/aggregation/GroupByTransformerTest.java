package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.aggregation.GroupByTransformer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GroupByTransformerTest {

    private GroupByTransformer groupByTransformer;

    @BeforeEach
    void setUp() {
        groupByTransformer = new GroupByTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("group_by", groupByTransformer.getName());
    }

    @Test
    void testTransformGroupByField() {
        List<Map<String, Object>> input = Arrays.asList(
                Map.of("name", "John", "department", "IT", "age", 30),
                Map.of("name", "Jane", "department", "HR", "age", 25),
                Map.of("name", "Bob", "department", "IT", "age", 35)
        );

        Map<String, Object> params = Map.of("groupBy", "department");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(2, result.size());

        Map<String, Object> itGroup = result.stream()
                .filter(r -> "IT".equals(r.get("department")))
                .findFirst()
                .orElseThrow();

        assertEquals(1, itGroup.size());
        assertEquals("IT", itGroup.get("department"));
    }

    @Test
    void testTransformGroupByMultipleFields() {
        List<Map<String, Object>> input = Arrays.asList(
                Map.of("department", "IT", "level", "Junior"),
                Map.of("department", "IT", "level", "Senior"),
                Map.of("department", "HR", "level", "Junior")
        );

        Map<String, Object> params = Map.of("groupBy", Arrays.asList("department", "level"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(3, result.size());

        Map<String, Object> itJunior = result.stream()
                .filter(r -> "IT".equals(r.get("department")) && "Junior".equals(r.get("level")))
                .findFirst()
                .orElseThrow();

        assertEquals(Map.of("department", "IT", "level", "Junior"), itJunior);
    }

    @Test
    void testTransformWithNullValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("department", null); // Null department

        List<Map<String, Object>> input = Arrays.asList(
                Map.of("name", "John", "department", "IT"),
                data,
                Map.of("name", "Bob", "department", "IT"),
                Map.of("name", "Alice") // Missing department field
        );

        Map<String, Object> params = Map.of("groupBy", "department");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> "IT".equals(r.get("department"))));
        assertTrue(result.stream().anyMatch(r -> "null".equals(r.get("department"))));
    }

    @Test
    void testTransformWithEmptyList() {
        List<Map<String, Object>> input = new ArrayList<>();
        Map<String, Object> params = Map.of("groupBy", "department");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertTrue(result.isEmpty());
    }

    @Test
    void testTransformWithNonListInput() {
        Map<String, Object> params = Map.of("groupBy", "department");

        assertThrows(DataTransformerExecutorException.class, () -> groupByTransformer.transform("not a list", params));
    }

    @Test
    void testTransformWithMissingGroupByParam() {
        List<Map<String, Object>> input = List.of(Map.of("name", "John", "department", "IT"));
        Map<String, Object> params = new HashMap<>();

        assertThrows(DataTransformerExecutorException.class, () -> groupByTransformer.transform(input, params));
    }

    @Test
    void testTransformWithNullParams() {
        List<Map<String, Object>> input = List.of(Map.of("name", "John", "department", "IT"));

        assertThrows(DataTransformerExecutorException.class, () -> groupByTransformer.transform(input, null));
    }

    @Test
    void testTransformWithInvalidGroupByParameter() {
        List<Map<String, Object>> input = List.of(Map.of("name", "John", "department", "IT"));
        Map<String, Object> params = Map.of("groupBy", 123);

        assertThrows(DataTransformerExecutorException.class, () -> groupByTransformer.transform(input, params));
    }
}

