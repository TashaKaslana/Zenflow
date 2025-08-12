package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetFieldTransformerTest {

    private SetFieldTransformer setFieldTransformer;

    @BeforeEach
    void setUp() {
        setFieldTransformer = new SetFieldTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("set_field", setFieldTransformer.getName());
    }

    @Test
    void testTransformAddNewField() {
        Map<String, Object> data = Map.of("name", "John", "age", 30);
        Map<String, Object> params = Map.of("field", "city", "value", "New York");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) setFieldTransformer.transform(data, params);

        assertEquals(3, result.size());
        assertEquals("John", result.get("name"));
        assertEquals(30, result.get("age"));
        assertEquals("New York", result.get("city"));
    }

    @Test
    void testTransformUpdateExistingField() {
        Map<String, Object> data = Map.of("name", "John", "age", 30, "city", "Boston");
        Map<String, Object> params = Map.of("field", "city", "value", "New York");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) setFieldTransformer.transform(data, params);

        assertEquals(3, result.size());
        assertEquals("John", result.get("name"));
        assertEquals(30, result.get("age"));
        assertEquals("New York", result.get("city"));
    }

    @Test
    void testTransformWithNullValue() {
        Map<String, Object> data = Map.of("name", "John", "age", 30);
        Map<String, Object> params = new HashMap<>();
        params.put("field", "city");
        params.put("value", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) setFieldTransformer.transform(data, params);

        assertEquals(3, result.size());
        assertNull(result.get("city"));
    }

    @Test
    void testTransformWithComplexValue() {
        Map<String, Object> address = Map.of("street", "123 Main St", "zipcode", "12345");
        Map<String, Object> data = Map.of("name", "John", "age", 30);
        Map<String, Object> params = Map.of("field", "address", "value", address);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) setFieldTransformer.transform(data, params);

        assertEquals(3, result.size());
        assertEquals(address, result.get("address"));
    }

    @Test
    void testTransformWithEmptyMap() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> params = Map.of("field", "name", "value", "John");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) setFieldTransformer.transform(data, params);

        assertEquals(1, result.size());
        assertEquals("John", result.get("name"));
    }

    @Test
    void testTransformWithNonMapData() {
        Map<String, Object> params = Map.of("field", "name", "value", "John");

        assertThrows(DataTransformerExecutorException.class, () -> setFieldTransformer.transform("not a map", params));
    }

    @Test
    void testTransformWithMissingFieldParam() {
        Map<String, Object> data = Map.of("name", "John");
        Map<String, Object> params = Map.of("value", "New York");

        assertThrows(DataTransformerExecutorException.class, () -> setFieldTransformer.transform(data, params));
    }

    @Test
    void testTransformWithMissingValueParam() {
        Map<String, Object> data = Map.of("name", "John");
        Map<String, Object> params = Map.of("field", "city");

        assertThrows(DataTransformerExecutorException.class, () -> setFieldTransformer.transform(data, params));
    }

    @Test
    void testTransformWithNullParams() {
        Map<String, Object> data = Map.of("name", "John");

        assertThrows(DataTransformerExecutorException.class, () -> setFieldTransformer.transform(data, null));
    }

    @Test
    void testTransformDoesNotModifyOriginal() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 30);
        Map<String, Object> params = Map.of("field", "city", "value", "New York");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) setFieldTransformer.transform(data, params);

        // Original data should not be modified
        assertEquals(2, data.size());
        assertFalse(data.containsKey("city"));

        // Result should contain the new field
        assertEquals(3, result.size());
        assertEquals("New York", result.get("city"));
    }
}
