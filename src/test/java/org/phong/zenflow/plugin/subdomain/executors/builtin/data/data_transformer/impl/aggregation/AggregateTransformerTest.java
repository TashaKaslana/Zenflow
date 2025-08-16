package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.aggregation.AggregateTransformer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregateTransformerTest {

    private AggregateTransformer aggregateTransformer;

    @BeforeEach
    void setUp() {
        aggregateTransformer = new AggregateTransformer();
    }

    @Test
    void testTransformAggregations() {
        List<Map<String, Object>> groups = Arrays.asList(
                Map.of("name", "John", "department", "IT", "age", 30, "salary", 50000),
                Map.of("name", "Bob", "department", "IT", "age", 35, "salary", 60000)
        );

        Map<String, Object> params = Map.of(
                "aggregations", Arrays.asList(
                        Map.of("field", "salary", "function", "sum", "alias", "total_salary"),
                        Map.of("field", "age", "function", "avg", "alias", "avg_age"),
                        Map.of("field", "name", "function", "count", "alias", "employee_count")
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) aggregateTransformer.transform(groups, params);

        assertEquals(3, result.size());

        assertEquals(110000.0, result.get("total_salary"));
        assertEquals(32.5, result.get("avg_age"));
        assertEquals(2, result.get("employee_count"));
    }

    @Test
    void testTransformWithDifferentFunctions() {
        List<Map<String, Object>> groups = List.of(
                Map.of("department", "IT", "salary", 50000, "bonus", 5000),
                Map.of("department", "IT", "salary", 60000, "bonus", 6000),
                Map.of("department", "IT", "salary", 55000, "bonus", 5500)
        );

        Map<String, Object> params = Map.of(
                "aggregations", Arrays.asList(
                        Map.of("field", "salary", "function", "min", "alias", "min_salary"),
                        Map.of("field", "salary", "function", "max", "alias", "max_salary"),
                        Map.of("field", "bonus", "function", "sum", "alias", "total_bonus")
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) aggregateTransformer.transform(groups, params);

        assertEquals(50000, result.get("min_salary"));
        assertEquals(60000, result.get("max_salary"));
        assertEquals(16500.0, result.get("total_bonus"));
    }
}

