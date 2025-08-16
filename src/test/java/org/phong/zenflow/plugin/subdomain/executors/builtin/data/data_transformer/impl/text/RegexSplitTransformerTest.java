package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.text;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.data.data_transformer.impl.text.RegexSplitTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegexSplitTransformerTest {

    private RegexSplitTransformer regexSplitTransformer;

    @BeforeEach
    void setUp() {
        regexSplitTransformer = new RegexSplitTransformer();
    }

    @Test
    void testGetName() {
        assertEquals("regex_split", regexSplitTransformer.getName());
    }

    @Test
    void testTransformWithCommaDelimiter() {
        Map<String, Object> params = Map.of("pattern", ",");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("apple,banana,cherry", params);

        assertEquals(3, result.size());
        assertEquals("apple", result.get(0));
        assertEquals("banana", result.get(1));
        assertEquals("cherry", result.get(2));
    }

    @Test
    void testTransformWithSpaceDelimiter() {
        Map<String, Object> params = Map.of("pattern", "\\s+");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("hello   world   test", params);

        assertEquals(3, result.size());
        assertEquals("hello", result.get(0));
        assertEquals("world", result.get(1));
        assertEquals("test", result.get(2));
    }

    @Test
    void testTransformWithPipeDelimiter() {
        Map<String, Object> params = Map.of("pattern", "\\|");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("name|age|city", params);

        assertEquals(3, result.size());
        assertEquals("name", result.get(0));
        assertEquals("age", result.get(1));
        assertEquals("city", result.get(2));
    }

    @Test
    void testTransformWithMultipleDelimiters() {
        Map<String, Object> params = Map.of("pattern", ",");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("a,b,c,d", params);

        assertEquals(4, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
        assertEquals("d", result.get(3));
    }

    @Test
    void testTransformWithNoMatches() {
        Map<String, Object> params = Map.of("pattern", ",");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("no commas here", params);

        assertEquals(1, result.size());
        assertEquals("no commas here", result.getFirst());
    }

    @Test
    void testTransformWithEmptyString() {
        Map<String, Object> params = Map.of("pattern", ",");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("", params);

        assertEquals(1, result.size());
        assertEquals("", result.getFirst());
    }

    @Test
    void testTransformWithNull() {
        Map<String, Object> params = Map.of("pattern", ",");
        Object result = regexSplitTransformer.transform(null, params);
        assertNull(result);
    }

    @Test
    void testTransformWithMissingPattern() {
        Map<String, Object> params = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> regexSplitTransformer.transform("test,data", params));
    }

    @Test
    void testTransformWithNullParams() {
        assertThrows(NullPointerException.class, () -> regexSplitTransformer.transform("test,data", null));
    }

    @Test
    void testTransformWithComplexRegex() {
        Map<String, Object> params = Map.of("pattern", "\\d+");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("abc123def456ghi", params);

        assertEquals(3, result.size());
        assertEquals("abc", result.get(0));
        assertEquals("def", result.get(1));
        assertEquals("ghi", result.get(2));
    }

    @Test
    void testTransformWithConsecutiveDelimiters() {
        Map<String, Object> params = Map.of("pattern", ",");
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) regexSplitTransformer.transform("a,,b,,,c", params);

        assertEquals(6, result.size());
        assertEquals("a", result.get(0));
        assertEquals("", result.get(1));
        assertEquals("b", result.get(2));
        assertEquals("", result.get(3));
        assertEquals("", result.get(4));
        assertEquals("c", result.get(5));
    }
}
