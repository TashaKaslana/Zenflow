package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.formatting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.impl.formatting.FormatDateTransformer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FormatDateTransformerTest {

    private FormatDateTransformer formatDateTransformer;

    @BeforeEach
    void setUp() {
        formatDateTransformer = new FormatDateTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("format_date", formatDateTransformer.getName());
    }

    @Test
    void testTransformWithMapAndDateField() {
        Map<String, Object> data = Map.of("createdAt", "2023-12-25 10:30:00", "name", "John");
        Map<String, Object> params = Map.of("field", "createdAt", "pattern", "yyyy-MM-dd HH:mm:ss");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatDateTransformer.transform(data, params);

        assertNotNull(result);
        assertEquals("John", result.get("name"));
        assertNotNull(result.get("createdAt"));
    }

    @Test
    void testTransformWithCustomPattern() {
        Map<String, Object> data = Map.of("date", "2023-12-25 15:45:30");
        Map<String, Object> params = Map.of("field", "date", "pattern", "dd/MM/yyyy HH:mm");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatDateTransformer.transform(data, params);

        assertNotNull(result);
        assertNotNull(result.get("date"));
    }

    @Test
    void testTransformWithTimestampInput() {
        // Test with timestamp (long) input
        Map<String, Object> data = Map.of("timestamp", System.currentTimeMillis());
        Map<String, Object> params = Map.of("field", "timestamp", "pattern", "yyyy-MM-dd HH:mm:ss");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatDateTransformer.transform(data, params);

        assertNotNull(result);
        assertNotNull(result.get("timestamp"));
        assertInstanceOf(String.class, result.get("timestamp"));
    }

    @Test
    void testTransformWithMissingField() {
        Map<String, Object> data = Map.of("name", "John");
        Map<String, Object> params = Map.of("field", "date", "pattern", "yyyy-MM-dd");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatDateTransformer.transform(data, params);

        assertNotNull(result);
        assertEquals("John", result.get("name"));
        assertNull(result.get("date"));
    }

    @Test
    void testTransformWithMissingFieldParam() {
        Map<String, Object> data = Map.of("date", "2023-12-25");
        Map<String, Object> params = Map.of("pattern", "yyyy-MM-dd");

        assertThrows(Exception.class, () -> formatDateTransformer.transform(data, params));
    }

    @Test
    void testTransformWithNullParams() {
        Map<String, Object> data = Map.of("date", "2023-12-25");

        assertThrows(Exception.class, () -> formatDateTransformer.transform(data, null));
    }
}
