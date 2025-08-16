package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.string;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.string.ConcatTransformer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConcatTransformerTest {

    private ConcatTransformer concatTransformer;

    @BeforeEach
    void setUp() {
        concatTransformer = new ConcatTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("concat", concatTransformer.getName());
    }

    @Test
    void testTransformWithSuffix() {
        Map<String, Object> params = Map.of("suffix", " world");
        String result = (String) concatTransformer.transform("hello", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformWithNumberSuffix() {
        Map<String, Object> params = Map.of("suffix", 123);
        String result = (String) concatTransformer.transform("value", params);
        assertEquals("value123", result);
    }

    @Test
    void testTransformWithNullData() {
        Map<String, Object> params = Map.of("suffix", " world");
        String result = (String) concatTransformer.transform(null, params);
        assertEquals("null world", result);
    }

    @Test
    void testTransformWithEmptyString() {
        Map<String, Object> params = Map.of("suffix", "suffix");
        String result = (String) concatTransformer.transform("", params);
        assertEquals("suffix", result);
    }

    @Test
    void testTransformWithEmptySuffix() {
        Map<String, Object> params = Map.of("suffix", "");
        String result = (String) concatTransformer.transform("hello", params);
        assertEquals("hello", result);
    }

    @Test
    void testTransformWithNumber() {
        Map<String, Object> params = Map.of("suffix", ".txt");
        String result = (String) concatTransformer.transform(123, params);
        assertEquals("123.txt", result);
    }

    @Test
    void testTransformWithoutSuffixParam() {
        Map<String, Object> params = new HashMap<>();
        assertThrows(DataTransformerExecutorException.class, () -> concatTransformer.transform("hello", params));
    }

    @Test
    void testTransformWithNullParams() {
        assertThrows(DataTransformerExecutorException.class, () -> concatTransformer.transform("hello", null));
    }

    @Test
    void testTransformWithSpecialCharacters() {
        Map<String, Object> params = Map.of("suffix", "@#$%");
        String result = (String) concatTransformer.transform("test", params);
        assertEquals("test@#$%", result);
    }
}
