package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.executor;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.registry.TransformerRegistry;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataTransformerExecutorTest {

    @Mock
    private TransformerRegistry registry;

    @Mock
    private DataTransformer trimTransformer;

    @Mock
    private DataTransformer uppercaseTransformer;

    @Mock
    private DataTransformer concatTransformer;

    @Mock
    private DataTransformer setFieldTransformer;

    @Mock
    private DataTransformer getFieldTransformer;

    @Mock
    private DataTransformer filterTransformer;

    @Mock
    private DataTransformer sortTransformer;

    @Mock
    private DataTransformer groupByTransformer;

    @Mock
    private DataTransformer formatNumberTransformer;

    @Mock
    private DataTransformer toJsonTransformer;

    @Mock
    private RuntimeContext runtimeContext;

    private DataTransformerExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DataTransformerExecutor(registry);
    }

    @Test
    void testExecutorKey() {
        assertEquals("core:data.transformer:1.0.0", executor.key());
    }

    @Test
    void testSingleTransformExecution() {
        List<Map<String, Object>> data = List.of(
                Map.of("value", 1),
                Map.of("value", 2)
        );
        Map<String, Object> params = Map.of(
                "expression", "value > 1",
                "mode", "include"
        );
        Map<String, Object> input = Map.of(
                "name", "filter",
                "data", data,
                "params", params
        );
        WorkflowConfig config = new WorkflowConfig(input);

        when(registry.getTransformer("filter")).thenReturn(filterTransformer);
        when(filterTransformer.transform(data, params)).thenReturn(List.of(Map.of("value", 2)));

        ExecutionResult result = executor.execute(config, runtimeContext);
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals(List.of(Map.of("value", 2)), result.getOutput().get("result"));
    }

    // ===========================================
    // BASIC PIPELINE TESTS
    // ===========================================

    @Test
    void testSimpleStringProcessingPipeline() {
        // Pipeline: trim -> uppercase -> concat
        List<Map<String, Object>> steps = Arrays.asList(
            Map.of("transformer", "trim", "params", Map.of()),
            Map.of("transformer", "uppercase", "params", Map.of()),
            Map.of("transformer", "concat", "params", Map.of("suffix", "!"))
        );

        Map<String, Object> input = Map.of(
            "data", "  hello world  ",
            "isPipeline", true,
            "steps", steps
        );
        WorkflowConfig config = new WorkflowConfig(input);

        // Mock transformer registry
        when(registry.getTransformer("trim")).thenReturn(trimTransformer);
        when(registry.getTransformer("uppercase")).thenReturn(uppercaseTransformer);
        when(registry.getTransformer("concat")).thenReturn(concatTransformer);

        // Mock pipeline transformation chain
        when(trimTransformer.transform("  hello world  ", Map.of())).thenReturn("hello world");
        when(uppercaseTransformer.transform("hello world", Map.of())).thenReturn("HELLO WORLD");
        when(concatTransformer.transform("HELLO WORLD", Map.of("suffix", "!"))).thenReturn("HELLO WORLD!");

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("HELLO WORLD!", result.getOutput().get("result"));

        // Verify pipeline execution order
        verify(trimTransformer).transform("  hello world  ", Map.of());
        verify(uppercaseTransformer).transform("hello world", Map.of());
        verify(concatTransformer).transform("HELLO WORLD", Map.of("suffix", "!"));
    }

    @Test
    void testDataEnrichmentPipeline() {
        // Pipeline: set_field (add timestamp) -> set_field (add processed flag) -> get_field (extract name)
        Map<String, Object> originalData = Map.of("name", "John", "age", 30);

        WorkflowConfig config = getWorkflowConfig(originalData);

        when(registry.getTransformer("set_field")).thenReturn(setFieldTransformer);
        when(registry.getTransformer("get_field")).thenReturn(getFieldTransformer);

        // Mock pipeline transformations
        Map<String, Object> afterFirstSet = new HashMap<>(originalData);
        afterFirstSet.put("timestamp", "2024-01-01T10:00:00");

        Map<String, Object> afterSecondSet = new HashMap<>(afterFirstSet);
        afterSecondSet.put("processed", true);

        when(setFieldTransformer.transform(eq(originalData), any())).thenReturn(afterFirstSet);
        when(setFieldTransformer.transform(eq(afterFirstSet), any())).thenReturn(afterSecondSet);
        when(getFieldTransformer.transform(eq(afterSecondSet), any())).thenReturn("John");

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("John", result.getOutput().get("result"));
    }

    private static @NotNull WorkflowConfig getWorkflowConfig(Map<String, Object> originalData) {
        List<Map<String, Object>> steps = Arrays.asList(
            Map.of("transformer", "set_field", "params", Map.of("field", "timestamp", "value", "2024-01-01T10:00:00")),
            Map.of("transformer", "set_field", "params", Map.of("field", "processed", "value", true)),
            Map.of("transformer", "get_field", "params", Map.of("field", "name"))
        );

        Map<String, Object> input = Map.of(
            "data", originalData,
            "isPipeline", true,
            "steps", steps
        );

        return new WorkflowConfig(input);
    }

    // ===========================================
    // DATA AGGREGATION PIPELINE TESTS
    // ===========================================

    @Test
    void testDataAnalyticsPipeline() {
        // Pipeline: filter (active users) -> sort (by salary desc) -> group_by (department with aggregations)
        List<Map<String, Object>> employees = Arrays.asList(
            Map.of("name", "John", "department", "IT", "salary", 70000, "active", true),
            Map.of("name", "Jane", "department", "HR", "salary", 60000, "active", false),
            Map.of("name", "Bob", "department", "IT", "salary", 80000, "active", true),
            Map.of("name", "Alice", "department", "HR", "salary", 65000, "active", true)
        );

        WorkflowConfig config = getWorkflowConfigForGroupBy(employees);

        when(registry.getTransformer("filter")).thenReturn(filterTransformer);
        when(registry.getTransformer("sort")).thenReturn(sortTransformer);
        when(registry.getTransformer("group_by")).thenReturn(groupByTransformer);

        // Mock pipeline transformations
        List<Map<String, Object>> afterFilter = Arrays.asList(
            Map.of("name", "John", "department", "IT", "salary", 70000, "active", true),
            Map.of("name", "Bob", "department", "IT", "salary", 80000, "active", true),
            Map.of("name", "Alice", "department", "HR", "salary", 65000, "active", true)
        );

        List<Map<String, Object>> afterSort = Arrays.asList(
            Map.of("name", "Bob", "department", "IT", "salary", 80000, "active", true),
            Map.of("name", "John", "department", "IT", "salary", 70000, "active", true),
            Map.of("name", "Alice", "department", "HR", "salary", 65000, "active", true)
        );

        List<Map<String, Object>> finalResult = Arrays.asList(
            Map.of("department", "IT", "avg_salary", 75000.0, "employee_count", 2),
            Map.of("department", "HR", "avg_salary", 65000.0, "employee_count", 1)
        );

        when(filterTransformer.transform(eq(employees), any())).thenReturn(afterFilter);
        when(sortTransformer.transform(eq(afterFilter), any())).thenReturn(afterSort);
        when(groupByTransformer.transform(eq(afterSort), any())).thenReturn(finalResult);

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals(finalResult, result.getOutput().get("result"));
    }

    private static @NotNull WorkflowConfig getWorkflowConfigForGroupBy(List<Map<String, Object>> employees) {
        List<Map<String, Object>> steps = Arrays.asList(
            Map.of("transformer", "filter", "params", Map.of("expression", "active == true", "mode", "include")),
            Map.of("transformer", "sort", "params", Map.of("field", "salary", "direction", "desc")),
            Map.of("transformer", "group_by", "params", Map.of(
                "groupBy", "department",
                "aggregations", Arrays.asList(
                    Map.of("field", "salary", "function", "avg", "alias", "avg_salary"),
                    Map.of("field", "name", "function", "count", "alias", "employee_count")
                )
            ))
        );

        Map<String, Object> input = Map.of(
            "data", employees,
            "isPipeline", true,
            "steps", steps
        );
        return new WorkflowConfig(input);
    }

    // ===========================================
    // FORMAT & EXPORT PIPELINE TESTS
    // ===========================================

    @Test
    void testDataFormattingAndExportPipeline() {
        // Pipeline: format_number (salary) -> set_field (add formatted date) -> to_json
        Map<String, Object> employeeData = Map.of(
            "name", "John",
            "salary", 75000,
            "department", "Engineering"
        );

        List<Map<String, Object>> steps = Arrays.asList(
            Map.of("transformer", "format_number", "params", Map.of(
                "field", "salary",
                "type", "currency"
            )),
            Map.of("transformer", "set_field", "params", Map.of(
                "field", "report_date",
                "value", "2024-01-01"
            )),
            Map.of("transformer", "to_json", "params", Map.of())
        );

        Map<String, Object> input = Map.of(
            "data", employeeData,
            "isPipeline", true,
            "steps", steps
        );
        WorkflowConfig config = new WorkflowConfig(input);

        when(registry.getTransformer("format_number")).thenReturn(formatNumberTransformer);
        when(registry.getTransformer("set_field")).thenReturn(setFieldTransformer);
        when(registry.getTransformer("to_json")).thenReturn(toJsonTransformer);

        // Mock pipeline transformations
        Map<String, Object> afterFormatNumber = Map.of(
            "name", "John",
            "salary", "$75,000.00",
            "department", "Engineering"
        );

        Map<String, Object> afterSetField = Map.of(
            "name", "John",
            "salary", "$75,000.00",
            "department", "Engineering",
            "report_date", "2024-01-01"
        );

        String jsonResult = "{\"name\":\"John\",\"salary\":\"$75,000.00\",\"department\":\"Engineering\",\"report_date\":\"2024-01-01\"}";

        when(formatNumberTransformer.transform(eq(employeeData), any())).thenReturn(afterFormatNumber);
        when(setFieldTransformer.transform(eq(afterFormatNumber), any())).thenReturn(afterSetField);
        when(toJsonTransformer.transform(eq(afterSetField), any())).thenReturn(jsonResult);

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals(jsonResult, result.getOutput().get("result"));
    }

    // ===========================================
    // FOREACH + PIPELINE COMBINATION TESTS
    // ===========================================

    @Test
    void testForEachWithComplexPipeline() {
        // Apply pipeline to each employee: trim name -> uppercase -> concat title
        List<Map<String, Object>> employees = Arrays.asList(
            Map.of("name", "  john  ", "department", "IT"),
            Map.of("name", "  jane  ", "department", "HR"),
            Map.of("name", "  bob   ", "department", "Finance")
        );

        List<Map<String, Object>> steps = Arrays.asList(
            Map.of("transformer", "get_field", "params", Map.of("field", "name")),
            Map.of("transformer", "trim", "params", Map.of()),
            Map.of("transformer", "uppercase", "params", Map.of()),
            Map.of("transformer", "concat", "params", Map.of("suffix", " - Employee"))
        );

        Map<String, Object> input = Map.of(
            "data", employees,
            "isPipeline", true,
            "forEach", true,
            "steps", steps
        );
        WorkflowConfig config = new WorkflowConfig(input);

        when(registry.getTransformer("get_field")).thenReturn(getFieldTransformer);
        when(registry.getTransformer("trim")).thenReturn(trimTransformer);
        when(registry.getTransformer("uppercase")).thenReturn(uppercaseTransformer);
        when(registry.getTransformer("concat")).thenReturn(concatTransformer);

        // Mock pipeline for first employee
        when(getFieldTransformer.transform(eq(employees.getFirst()), any())).thenReturn("  john  ");
        when(trimTransformer.transform("  john  ", Map.of())).thenReturn("john");
        when(uppercaseTransformer.transform("john", Map.of())).thenReturn("JOHN");
        when(concatTransformer.transform("JOHN", Map.of("suffix", " - Employee"))).thenReturn("JOHN - Employee");

        // Mock pipeline for second employee
        when(getFieldTransformer.transform(eq(employees.get(1)), any())).thenReturn("  jane  ");
        when(trimTransformer.transform("  jane  ", Map.of())).thenReturn("jane");
        when(uppercaseTransformer.transform("jane", Map.of())).thenReturn("JANE");
        when(concatTransformer.transform("JANE", Map.of("suffix", " - Employee"))).thenReturn("JANE - Employee");

        // Mock pipeline for third employee
        when(getFieldTransformer.transform(eq(employees.get(2)), any())).thenReturn("  bob   ");
        when(trimTransformer.transform("  bob   ", Map.of())).thenReturn("bob");
        when(uppercaseTransformer.transform("bob", Map.of())).thenReturn("BOB");
        when(concatTransformer.transform("BOB", Map.of("suffix", " - Employee"))).thenReturn("BOB - Employee");

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result.getOutput().get("result");

        assertEquals(3, resultList.size());
        assertEquals("JOHN - Employee", resultList.get(0));
        assertEquals("JANE - Employee", resultList.get(1));
        assertEquals("BOB - Employee", resultList.get(2));
    }

    @Test
    void testComplexDataProcessingWorkflow() {
        // Real-world scenario: Process sales data
        // 1. Filter active sales
        // 2. Add calculated fields (tax, total)
        // 3. Sort by total descending
        // 4. Format currency values
        // 5. Export to JSON

        List<Map<String, Object>> salesData = Arrays.asList(
            Map.of("id", 1, "amount", 1000, "active", true, "customer", "Acme Corp"),
            Map.of("id", 2, "amount", 500, "active", false, "customer", "Beta Inc"),
            Map.of("id", 3, "amount", 1500, "active", true, "customer", "Gamma LLC")
        );

        List<Map<String, Object>> steps = Arrays.asList(
            Map.of("transformer", "filter", "params", Map.of("expression", "active == true", "mode", "include")),
            Map.of("transformer", "set_field", "params", Map.of("field", "tax", "value", "calculated_tax")),
            Map.of("transformer", "set_field", "params", Map.of("field", "total", "value", "calculated_total")),
            Map.of("transformer", "sort", "params", Map.of("field", "total", "direction", "desc")),
            Map.of("transformer", "format_number", "params", Map.of("field", "amount", "type", "currency")),
            Map.of("transformer", "to_json", "params", Map.of())
        );

        Map<String, Object> input = Map.of(
            "data", salesData,
            "isPipeline", true,
            "steps", steps
        );
        WorkflowConfig config = new WorkflowConfig(input);

        when(registry.getTransformer("filter")).thenReturn(filterTransformer);
        when(registry.getTransformer("set_field")).thenReturn(setFieldTransformer);
        when(registry.getTransformer("sort")).thenReturn(sortTransformer);
        when(registry.getTransformer("format_number")).thenReturn(formatNumberTransformer);
        when(registry.getTransformer("to_json")).thenReturn(toJsonTransformer);

        // Mock the complex pipeline execution
        List<Map<String, Object>> filteredData = Arrays.asList(
            Map.of("id", 1, "amount", 1000, "active", true, "customer", "Acme Corp"),
            Map.of("id", 3, "amount", 1500, "active", true, "customer", "Gamma LLC")
        );

        String finalJson = "[{\"id\":3,\"amount\":\"$1,500.00\",\"customer\":\"Gamma LLC\"},{\"id\":1,\"amount\":\"$1,000.00\",\"customer\":\"Acme Corp\"}]";

        when(filterTransformer.transform(eq(salesData), any())).thenReturn(filteredData);
        when(setFieldTransformer.transform(any(), any())).thenAnswer(invocation -> {
            // Mock adding fields
            return invocation.getArgument(0);
        });
        when(sortTransformer.transform(any(), any())).thenAnswer(invocation -> {
            // Mock sorting
            return invocation.getArgument(0);
        });
        when(formatNumberTransformer.transform(any(), any())).thenAnswer(invocation -> {
            // Mock number formatting
            return invocation.getArgument(0);
        });
        when(toJsonTransformer.transform(any(), any())).thenReturn(finalJson);

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals(finalJson, result.getOutput().get("result"));

        // Verify all transformers were called in sequence
        verify(filterTransformer, times(1)).transform(any(), any());
        verify(setFieldTransformer, times(2)).transform(any(), any()); // Called twice for tax and total
        verify(sortTransformer, times(1)).transform(any(), any());
        verify(formatNumberTransformer, times(1)).transform(any(), any());
        verify(toJsonTransformer, times(1)).transform(any(), any());
    }

    // ===========================================
    // ERROR HANDLING IN PIPELINES
    // ===========================================

    @Test
    void testPipelineWithMissingSteps() {
        Map<String, Object> input = Map.of(
            "data", "test data",
            "isPipeline", true
            // Missing "steps" parameter
        );
        WorkflowConfig config = new WorkflowConfig(input);

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertTrue(result.getError().contains("Pipeline steps are missing"));
    }

    @Test
    void testPipelineWithInvalidStepConfiguration() {
        List<Map<String, Object>> steps = List.of(
                Map.of("transformer", "", "params", Map.of()) // Empty transformer name
        );

        Map<String, Object> input = Map.of(
            "data", "test data",
            "isPipeline", true,
            "steps", steps
        );
        WorkflowConfig config = new WorkflowConfig(input);

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertTrue(result.getError().contains("Transformer name is missing"));
    }

    @Test
    void testPipelineWithMissingTransformerParams() {
        List<Map<String, Object>> steps = List.of(
                Map.of("transformer", "trim") // Missing params
        );

        Map<String, Object> input = Map.of(
            "data", "test data",
            "isPipeline", true,
            "steps", steps
        );
        WorkflowConfig config = new WorkflowConfig(input);

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertTrue(result.getError().contains("Params are missing"));
    }

    // ===========================================
    // SINGLE TRANSFORMER TESTS (for comparison)
    // ===========================================

    @Test
    void testSingleTransformerExecution() {
        Map<String, Object> input = Map.of(
            "name", "uppercase",
            "data", "hello world",
            "params", Map.of()
        );
        WorkflowConfig config = new WorkflowConfig(input);

        when(registry.getTransformer("uppercase")).thenReturn(uppercaseTransformer);
        when(uppercaseTransformer.transform("hello world", Map.of())).thenReturn("HELLO WORLD");

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("HELLO WORLD", result.getOutput().get("result"));
        verify(uppercaseTransformer).transform("hello world", Map.of());
    }

    @Test
    void testForEachSingleTransformer() {
        List<String> inputData = Arrays.asList("hello", "world", "test");
        Map<String, Object> input = Map.of(
            "name", "uppercase",
            "data", inputData,
            "params", Map.of(),
            "forEach", true
        );
        WorkflowConfig config = new WorkflowConfig(input);

        when(registry.getTransformer("uppercase")).thenReturn(uppercaseTransformer);
        when(uppercaseTransformer.transform("hello", Map.of())).thenReturn("HELLO");
        when(uppercaseTransformer.transform("world", Map.of())).thenReturn("WORLD");
        when(uppercaseTransformer.transform("test", Map.of())).thenReturn("TEST");

        ExecutionResult result = executor.execute(config, runtimeContext);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result.getOutput().get("result");
        assertEquals(Arrays.asList("HELLO", "WORLD", "TEST"), resultList);
    }
}
