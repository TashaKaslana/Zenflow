package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetFieldTransformerTest {

    private GetFieldTransformer getFieldTransformer;

    @BeforeEach
    void setUp() {
        getFieldTransformer = new GetFieldTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("get_field", getFieldTransformer.getName());
    }

    @Test
    void testTransformWithValidField() {
        Map<String, Object> data = Map.of("name", "John", "age", 30, "city", "New York");
        Map<String, Object> params = Map.of("field", "name");

        Object result = getFieldTransformer.transform(data, params);
        assertEquals("John", result);
    }

    @Test
    void testTransformWithNonExistentField() {
        Map<String, Object> data = Map.of("name", "John", "age", 30);
        Map<String, Object> params = Map.of("field", "address");

        Object result = getFieldTransformer.transform(data, params);
        assertNull(result);
    }

    @Test
    void testTransformWithNumericField() {
        Map<String, Object> data = Map.of("name", "John", "age", 30, "salary", 50000.0);
        Map<String, Object> params = Map.of("field", "age");

        Object result = getFieldTransformer.transform(data, params);
        assertEquals(30, result);
    }

    @Test
    void testTransformWithNestedMap() {
        Map<String, Object> nestedData = Map.of("street", "123 Main St", "zipcode", "12345");
        Map<String, Object> data = Map.of("name", "John", "address", nestedData);
        Map<String, Object> params = Map.of("field", "address");

        Object result = getFieldTransformer.transform(data, params);
        assertEquals(nestedData, result);
    }

    @Test
    void testTransformWithNonMapData() {
        Map<String, Object> params = Map.of("field", "name");

        assertThrows(DataTransformerExecutorException.class, () -> getFieldTransformer.transform("not a map", params));
    }

    @Test
    void testTransformWithNullData() {
        Map<String, Object> params = Map.of("field", "name");

        assertThrows(DataTransformerExecutorException.class, () -> getFieldTransformer.transform(null, params));
    }

    @Test
    void testTransformWithMissingFieldParam() {
        Map<String, Object> data = Map.of("name", "John");
        Map<String, Object> params = new HashMap<>();

        assertThrows(DataTransformerExecutorException.class, () -> getFieldTransformer.transform(data, params));
    }

    @Test
    void testTransformWithNullParams() {
        Map<String, Object> data = Map.of("name", "John");

        assertThrows(DataTransformerExecutorException.class, () -> getFieldTransformer.transform(data, null));
    }

    @Test
    void testTransformWithEmptyMap() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> params = Map.of("field", "name");

        Object result = getFieldTransformer.transform(data, params);
        assertNull(result);
    }
}
