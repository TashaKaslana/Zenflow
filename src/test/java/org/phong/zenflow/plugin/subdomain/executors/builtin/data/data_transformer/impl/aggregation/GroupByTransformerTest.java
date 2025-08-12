package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void testTransformGroupByFieldWithAggregations() {
        List<Map<String, Object>> input = Arrays.asList(
                Map.of("name", "John", "department", "IT", "age", 30, "salary", 50000),
                Map.of("name", "Jane", "department", "HR", "age", 25, "salary", 45000),
                Map.of("name", "Bob", "department", "IT", "age", 35, "salary", 60000),
                Map.of("name", "Alice", "department", "HR", "age", 28, "salary", 48000)
        );

        Map<String, Object> params = Map.of(
                "groupBy", "department",
                "aggregations", Arrays.asList(
                        Map.of("field", "salary", "function", "sum", "alias", "total_salary"),
                        Map.of("field", "age", "function", "avg", "alias", "avg_age"),
                        Map.of("field", "name", "function", "count", "alias", "employee_count")
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(2, result.size());

        // Find IT department result
        Map<String, Object> itResult = result.stream()
                .filter(r -> "IT".equals(r.get("department")))
                .findFirst()
                .orElseThrow();

        assertEquals("IT", itResult.get("department"));
        assertEquals(110000.0, itResult.get("total_salary")); // 50000 + 60000
        assertEquals(32.5, itResult.get("avg_age")); // (30 + 35) / 2
        assertEquals(2, itResult.get("employee_count"));

        // Find HR department result
        Map<String, Object> hrResult = result.stream()
                .filter(r -> "HR".equals(r.get("department")))
                .findFirst()
                .orElseThrow();

        assertEquals("HR", hrResult.get("department"));
        assertEquals(93000.0, hrResult.get("total_salary")); // 45000 + 48000
        assertEquals(26.5, hrResult.get("avg_age")); // (25 + 28) / 2
        assertEquals(2, hrResult.get("employee_count"));
    }

    @Test
    void testTransformGroupByMultipleFields() {
        List<Map<String, Object>> input = Arrays.asList(
                Map.of("department", "IT", "level", "Junior", "salary", 40000),
                Map.of("department", "IT", "level", "Senior", "salary", 70000),
                Map.of("department", "HR", "level", "Junior", "salary", 35000),
                Map.of("department", "HR", "level", "Senior", "salary", 65000)
        );

        Map<String, Object> params = Map.of(
                "groupBy", Arrays.asList("department", "level"),
                "aggregations", List.of(
                        Map.of("field", "salary", "function", "avg", "alias", "avg_salary")
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(4, result.size());

        // Verify one of the groups
        Map<String, Object> itJuniorResult = result.stream()
                .filter(r -> "IT".equals(r.get("department")) && "Junior".equals(r.get("level")))
                .findFirst()
                .orElseThrow();

        assertEquals("IT", itJuniorResult.get("department"));
        assertEquals("Junior", itJuniorResult.get("level"));
        assertEquals(40000.0, itJuniorResult.get("avg_salary"));
    }

    @Test
    void testTransformWithoutAggregations() {
        List<Map<String, Object>> input = Arrays.asList(
                Map.of("name", "John", "department", "IT"),
                Map.of("name", "Jane", "department", "HR"),
                Map.of("name", "Bob", "department", "IT")
        );

        Map<String, Object> params = Map.of("groupBy", "department");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(2, result.size());

        // Should still group but without aggregations
        assertTrue(result.stream().anyMatch(r -> "IT".equals(r.get("department"))));
        assertTrue(result.stream().anyMatch(r -> "HR".equals(r.get("department"))));
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

        Map<String, Object> params = Map.of(
                "groupBy", "department",
                "aggregations", List.of(
                        Map.of("field", "name", "function", "count", "alias", "count")
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(2, result.size());

        // Should have IT group and null group
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
        List<Map<String, Object>> input = List.of(
                Map.of("name", "John", "department", "IT")
        );
        Map<String, Object> params = new HashMap<>();

        assertThrows(DataTransformerExecutorException.class, () -> groupByTransformer.transform(input, params));
    }

    @Test
    void testTransformWithNullParams() {
        List<Map<String, Object>> input = List.of(
                Map.of("name", "John", "department", "IT")
        );

        assertThrows(DataTransformerExecutorException.class, () -> groupByTransformer.transform(input, null));
    }

    @Test
    void testTransformWithInvalidGroupByParameter() {
        List<Map<String, Object>> input = List.of(
                Map.of("name", "John", "department", "IT")
        );
        Map<String, Object> params = Map.of("groupBy", 123); // Invalid type

        assertThrows(DataTransformerExecutorException.class, () -> groupByTransformer.transform(input, params));
    }

    @Test
    void testTransformWithDifferentAggregationFunctions() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("department", "IT", "salary", 50000, "bonus", 5000),
            Map.of("department", "IT", "salary", 60000, "bonus", 6000),
            Map.of("department", "IT", "salary", 55000, "bonus", 5500)
        );

        Map<String, Object> params = Map.of(
            "groupBy", "department",
            "aggregations", Arrays.asList(
                Map.of("field", "salary", "function", "min", "alias", "min_salary"),
                Map.of("field", "salary", "function", "max", "alias", "max_salary"),
                Map.of("field", "bonus", "function", "sum", "alias", "total_bonus")
            )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) groupByTransformer.transform(input, params);

        assertEquals(1, result.size());

        Map<String, Object> itResult = result.getFirst();
        assertEquals("IT", itResult.get("department"));
        assertEquals(50000, itResult.get("min_salary")); // int, not double
        assertEquals(60000, itResult.get("max_salary")); // int, not double
        assertEquals(16500.0, itResult.get("total_bonus")); // double from sum function
    }
}
