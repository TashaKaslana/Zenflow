package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
                Map.of(
                        "department", "IT",
                        "items", Arrays.asList(
                                Map.of("name", "John", "department", "IT", "age", 30, "salary", 50000),
                                Map.of("name", "Bob", "department", "IT", "age", 35, "salary", 60000)
                        )
                ),
        Map.of(
                        "department", "HR",
                        "items", Arrays.asList(
                                Map.of("name", "Jane", "department", "HR", "age", 25, "salary", 45000),
                                Map.of("name", "Alice", "department", "HR", "age", 28, "salary", 48000)
                        )
                )
        );

        Map<String, Object> params = Map.of(
                "aggregations", Arrays.asList(
                        Map.of("field", "salary", "function", "sum", "alias", "total_salary"),
                        Map.of("field", "age", "function", "avg", "alias", "avg_age"),
                        Map.of("field", "name", "function", "count", "alias", "employee_count")
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) aggregateTransformer.transform(groups, params);

        assertEquals(2, result.size());

        Map<String, Object> itResult = result.stream()
                .filter(r -> "IT".equals(r.get("department")))
                .findFirst()
                .orElseThrow();

        assertEquals(110000.0, itResult.get("total_salary"));
        assertEquals(32.5, itResult.get("avg_age"));
        assertEquals(2, itResult.get("employee_count"));
    }

    @Test
    void testTransformWithDifferentFunctions() {
        List<Map<String, Object>> groups = List.of(
                Map.of(
                        "department", "IT",
                        "items", Arrays.asList(
                                Map.of("department", "IT", "salary", 50000, "bonus", 5000),
                                Map.of("department", "IT", "salary", 60000, "bonus", 6000),
                                Map.of("department", "IT", "salary", 55000, "bonus", 5500)
                        )
                )
        );

        Map<String, Object> params = Map.of(
                "aggregations", Arrays.asList(
                        Map.of("field", "salary", "function", "min", "alias", "min_salary"),
                        Map.of("field", "salary", "function", "max", "alias", "max_salary"),
                        Map.of("field", "bonus", "function", "sum", "alias", "total_bonus")
                )
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) aggregateTransformer.transform(groups, params);

        Map<String, Object> itResult = result.getFirst();
        assertEquals(50000, itResult.get("min_salary"));
        assertEquals(60000, itResult.get("max_salary"));
        assertEquals(16500.0, itResult.get("total_bonus"));
    }
}

