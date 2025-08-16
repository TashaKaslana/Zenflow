package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.string;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.string.LowercaseTransformer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LowercaseTransformerTest {

    private LowercaseTransformer lowercaseTransformer;

    @BeforeEach
    void setUp() {
        lowercaseTransformer = new LowercaseTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("to_lowercase", lowercaseTransformer.getName());
    }

    @Test
    void testTransformString() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) lowercaseTransformer.transform("HELLO WORLD", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformNumber() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) lowercaseTransformer.transform(123, params);
        assertEquals("123", result);
    }

    @Test
    void testTransformNull() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) lowercaseTransformer.transform(null, params);
        assertNull(result);
    }

    @Test
    void testTransformEmptyString() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) lowercaseTransformer.transform("", params);
        assertEquals("", result);
    }

    @Test
    void testTransformMixedCase() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) lowercaseTransformer.transform("HeLLo WoRLd", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformSpecialCharacters() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) lowercaseTransformer.transform("HELLO@WORLD#123", params);
        assertEquals("hello@world#123", result);
    }
}
