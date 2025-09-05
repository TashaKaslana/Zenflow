package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.aggregation.FilterTransformer;
import org.phong.zenflow.workflow.subdomain.execution.functions.AviatorFunctionRegistry;
import org.phong.zenflow.workflow.subdomain.execution.functions.StringContainsFunction;
import org.phong.zenflow.workflow.subdomain.execution.services.TemplateService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FilterTransformerTest {

    private FilterTransformer filterTransformer;

    @BeforeEach
    void setUp() {
        filterTransformer = new FilterTransformer(new TemplateService(new AviatorFunctionRegistry(List.of(new StringContainsFunction()))));
    }

    @Test
    void testGetName() {
        assertEquals("filter", filterTransformer.getName());
    }

    @Test
    void testFilterIncludeMode() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30, "active", true),
            Map.of("name", "Alice", "age", 25, "active", false),
            Map.of("name", "Bob", "age", 35, "active", true)
        );

        Map<String, Object> params = Map.of(
            "expression", "age > 28 && active == true",
            "mode", "include"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) filterTransformer.transform(input, params);

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).get("name"));
        assertEquals("Bob", result.get(1).get("name"));
    }

    @Test
    void testFilterExcludeMode() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Alice", "age", 25),
            Map.of("name", "Bob", "age", 35)
        );

        Map<String, Object> params = Map.of(
            "expression", "age < 30",
            "mode", "exclude"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) filterTransformer.transform(input, params);

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).get("name"));
        assertEquals("Bob", result.get(1).get("name"));
    }

    @Test
    void testFilterStringOperations() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John Doe", "email", "john@example.com", "age", 30),
            Map.of("name", "Alice Smith", "email", "alice@test.com", "age", 25),
            Map.of("name", "Bob Johnson", "email", "bob@example.com", "age", 35)
        );

        Map<String, Object> params = Map.of(
            "expression", "age > 25"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) filterTransformer.transform(input, params);

        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).get("name"));
        assertEquals("Bob Johnson", result.get(1).get("name"));
    }

    @Test
    void testFilterPrimitiveValues() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6);

        Map<String, Object> params = Map.of(
            "expression", "value % 2 == 0"
        );

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) filterTransformer.transform(input, params);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    void testFilterComplexExpression() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30, "department", "IT", "salary", 50000),
            Map.of("name", "Alice", "age", 25, "department", "HR", "salary", 45000),
            Map.of("name", "Bob", "age", 35, "department", "IT", "salary", 60000),
            Map.of("name", "Carol", "age", 28, "department", "Finance", "salary", 55000)
        );

        Map<String, Object> params = Map.of(
            "expression", "(department == 'IT' && salary > 50000) || (age < 30 && salary > 50000)"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) filterTransformer.transform(input, params);

        assertEquals(2, result.size());
        assertEquals("Bob", result.get(0).get("name"));
        assertEquals("Carol", result.get(1).get("name"));
    }

    @Test
    void testFilterEmptyResult() {
        List<Map<String, Object>> input = Arrays.asList(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Alice", "age", 25)
        );

        Map<String, Object> params = Map.of(
            "expression", "age > 50"
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) filterTransformer.transform(input, params);

        assertTrue(result.isEmpty());
    }

    @Test
    void testInvalidInputType() {
        assertThrows(DataTransformerExecutorException.class, () -> filterTransformer.transform("not a list", Map.of("expression", "true")));
    }

    @Test
    void testMissingExpression() {
        List<Map<String, Object>> input = List.of(Map.of("name", "John"));

        assertThrows(DataTransformerExecutorException.class, () -> filterTransformer.transform(input, null));

        assertThrows(DataTransformerExecutorException.class, () -> filterTransformer.transform(input, Map.of()));
    }

    @Test
    void testInvalidExpression() {
        List<Map<String, Object>> input = List.of(Map.of("name", "John"));

        assertThrows(DataTransformerExecutorException.class, () -> filterTransformer.transform(input, Map.of("expression", "invalid syntax +++")));
    }
}
