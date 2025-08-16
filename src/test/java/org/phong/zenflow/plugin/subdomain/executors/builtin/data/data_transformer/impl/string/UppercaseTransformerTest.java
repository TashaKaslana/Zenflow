package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.string;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.string.UppercaseTransformer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UppercaseTransformerTest {

    private UppercaseTransformer uppercaseTransformer;

    @BeforeEach
    void setUp() {
        uppercaseTransformer = new UppercaseTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("uppercase", uppercaseTransformer.getName());
    }

    @Test
    void testTransformString() {
        Map<String, Object> params = new HashMap<>();
        String result = uppercaseTransformer.transform("hello world", params);
        assertEquals("HELLO WORLD", result);
    }

    @Test
    void testTransformNumber() {
        Map<String, Object> params = new HashMap<>();
        String result = uppercaseTransformer.transform(123, params);
        assertEquals("123", result);
    }

    @Test
    void testTransformNull() {
        Map<String, Object> params = new HashMap<>();
        String result = uppercaseTransformer.transform(null, params);
        assertNull(result);
    }

    @Test
    void testTransformEmptyString() {
        Map<String, Object> params = new HashMap<>();
        String result = uppercaseTransformer.transform("", params);
        assertEquals("", result);
    }

    @Test
    void testTransformMixedCase() {
        Map<String, Object> params = new HashMap<>();
        String result = uppercaseTransformer.transform("HeLLo WoRLd", params);
        assertEquals("HELLO WORLD", result);
    }

    @Test
    void testTransformSpecialCharacters() {
        Map<String, Object> params = new HashMap<>();
        String result = uppercaseTransformer.transform("hello@world#123", params);
        assertEquals("HELLO@WORLD#123", result);
    }
}
