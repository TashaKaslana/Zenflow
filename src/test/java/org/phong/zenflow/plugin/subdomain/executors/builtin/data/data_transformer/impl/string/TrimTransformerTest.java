package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.string;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrimTransformerTest {

    private TrimTransformer trimTransformer;

    @BeforeEach
    void setUp() {
        trimTransformer = new TrimTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("trim", trimTransformer.getName());
    }

    @Test
    void testTransformWithLeadingSpaces() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform("   hello world", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformWithTrailingSpaces() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform("hello world   ", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformWithLeadingAndTrailingSpaces() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform("   hello world   ", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformWithTabs() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform("\t\thello world\t\t", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformWithNewlines() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform("\n\nhello world\n\n", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformWithMixedWhitespace() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform(" \t\n hello world \n\t ", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformNull() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform(null, params);
        assertNull(result);
    }

    @Test
    void testTransformEmptyString() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform("", params);
        assertEquals("", result);
    }

    @Test
    void testTransformOnlyWhitespace() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform("   \t\n   ", params);
        assertEquals("", result);
    }

    @Test
    void testTransformNumber() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) trimTransformer.transform(123, params);
        assertEquals("123", result);
    }
}
