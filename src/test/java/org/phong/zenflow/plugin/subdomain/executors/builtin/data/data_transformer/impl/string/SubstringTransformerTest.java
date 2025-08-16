package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.string;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.string.SubstringTransformer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubstringTransformerTest {

    private SubstringTransformer substringTransformer;

    @BeforeEach
    void setUp() {
        substringTransformer = new SubstringTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("substring", substringTransformer.getName());
    }

    @Test
    void testTransformWithStartAndEnd() {
        Map<String, Object> params = Map.of("start", 0, "end", 5);
        String result = (String) substringTransformer.transform("hello world", params);
        assertEquals("hello", result);
    }

    @Test
    void testTransformWithOnlyStart() {
        Map<String, Object> params = Map.of("start", 6);
        String result = (String) substringTransformer.transform("hello world", params);
        assertEquals("world", result);
    }

    @Test
    void testTransformWithOnlyEnd() {
        Map<String, Object> params = Map.of("end", 5);
        String result = (String) substringTransformer.transform("hello world", params);
        assertEquals("hello", result);
    }

    @Test
    void testTransformWithNoParams() {
        Map<String, Object> params = new HashMap<>();
        String result = (String) substringTransformer.transform("hello world", params);
        assertEquals("hello world", result);
    }

    @Test
    void testTransformNull() {
        Map<String, Object> params = Map.of("start", 0, "end", 5);
        Object result = substringTransformer.transform(null, params);
        assertNull(result);
    }

    @Test
    void testTransformEmptyString() {
        Map<String, Object> params = Map.of("start", 0, "end", 0);
        String result = (String) substringTransformer.transform("", params);
        assertEquals("", result);
    }

    @Test
    void testTransformWithNegativeStart() {
        Map<String, Object> params = Map.of("start", -1, "end", 5);
        assertThrows(StringIndexOutOfBoundsException.class, () -> substringTransformer.transform("hello world", params));
    }

    @Test
    void testTransformWithEndBeyondLength() {
        Map<String, Object> params = Map.of("start", 0, "end", 100);
        assertThrows(StringIndexOutOfBoundsException.class, () -> substringTransformer.transform("hello world", params));
    }

    @Test
    void testTransformWithStartGreaterThanEnd() {
        Map<String, Object> params = Map.of("start", 10, "end", 5);
        assertThrows(StringIndexOutOfBoundsException.class, () -> substringTransformer.transform("hello world", params));
    }

    @Test
    void testTransformNumber() {
        Map<String, Object> params = Map.of("start", 1, "end", 3);
        String result = (String) substringTransformer.transform(12345, params);
        assertEquals("23", result);
    }
}
