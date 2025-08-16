package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.formatting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.formatting.FormatNumberTransformer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatNumberTransformerTest {

    private FormatNumberTransformer formatNumberTransformer;

    @BeforeEach
    void setUp() {
        formatNumberTransformer = new FormatNumberTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("format_number", formatNumberTransformer.getName());
    }

    @Test
    void testTransformWithMapAndNumberField() {
        Map<String, Object> data = Map.of("price", 123.456789, "name", "Product");
        Map<String, Object> params = Map.of("field", "price", "pattern", "#.##");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatNumberTransformer.transform(data, params);

        assertNotNull(result);
        assertEquals("Product", result.get("name"));
        assertNotNull(result.get("price"));
    }

    @Test
    void testTransformWithDecimalType() {
        Map<String, Object> data = Map.of("amount", 1234.56);
        Map<String, Object> params = Map.of("field", "amount", "type", "decimal", "pattern", "#,##0.00");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatNumberTransformer.transform(data, params);

        assertNotNull(result);
        assertNotNull(result.get("amount"));
    }

    @Test
    void testTransformWithCurrencyType() {
        Map<String, Object> data = Map.of("price", 99.99);
        Map<String, Object> params = Map.of("field", "price", "type", "currency");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatNumberTransformer.transform(data, params);

        assertNotNull(result);
        assertNotNull(result.get("price"));
    }

    @Test
    void testTransformWithPercentType() {
        Map<String, Object> data = Map.of("rate", 0.75);
        Map<String, Object> params = Map.of("field", "rate", "type", "percent");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatNumberTransformer.transform(data, params);

        assertNotNull(result);
        assertNotNull(result.get("rate"));
    }

    @Test
    void testTransformWithMissingField() {
        Map<String, Object> data = Map.of("name", "Product");
        Map<String, Object> params = Map.of("field", "price", "pattern", "#.##");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) formatNumberTransformer.transform(data, params);

        assertNotNull(result);
        assertEquals("Product", result.get("name"));
        assertNull(result.get("price"));
    }

    @Test
    void testTransformWithMissingFieldParam() {
        Map<String, Object> data = Map.of("price", 123.45);
        Map<String, Object> params = Map.of("pattern", "#.##");

        assertThrows(DataTransformerExecutorException.class, () -> formatNumberTransformer.transform(data, params));
    }

    @Test
    void testTransformWithNullParams() {
        Map<String, Object> data = Map.of("price", 123.45);

        assertThrows(DataTransformerExecutorException.class, () -> formatNumberTransformer.transform(data, null));
    }
}
