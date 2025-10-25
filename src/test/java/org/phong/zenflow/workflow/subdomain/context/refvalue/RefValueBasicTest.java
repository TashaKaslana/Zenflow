package org.phong.zenflow.workflow.subdomain.context.refvalue;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.phong.zenflow.workflow.subdomain.context.refvalue.impl.FileRefValue;
import org.phong.zenflow.workflow.subdomain.context.refvalue.impl.JsonRefValue;
import org.phong.zenflow.workflow.subdomain.context.refvalue.impl.MemoryRefValue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for RefValue implementations.
 * Verifies core functionality: creation, read, streaming, cleanup.
 */
class RefValueBasicTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testMemoryRefValue() throws Exception {
        // Test with simple string
        try (MemoryRefValue memRef = new MemoryRefValue("test-value")) {
            assertEquals(RefValueType.MEMORY, memRef.getType());
            assertEquals("test-value", memRef.read(String.class));
            assertTrue(memRef.getSize() > 0);
        }
    }
    
    @Test
    void testMemoryRefValueWithMap() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 30);
        
        try (MemoryRefValue memRef = new MemoryRefValue(data)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = memRef.read(Map.class);
            assertEquals("John", result.get("name"));
            assertEquals(30, result.get("age"));
        }
    }
    
    @Test
    void testJsonRefValue() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("user", Map.of("name", "Alice", "email", "alice@example.com"));
        data.put("score", 95);
        
        JsonRefValue jsonRef = JsonRefValue.fromObject(data);
        assertEquals(RefValueType.JSON, jsonRef.getType());
        assertTrue(jsonRef.getMediaType().isPresent());
        assertEquals("application/json", jsonRef.getMediaType().get());
        
        // Test full materialization
        @SuppressWarnings("unchecked")
        Map<String, Object> result = jsonRef.read(Map.class);
        assertEquals(95, result.get("score"));
        
        // Test JsonPointer extraction
        String userName = jsonRef.getAt("/user/name", String.class);
        assertEquals("Alice", userName);
        
        // Test via ReadFunction
        String email = jsonRef.read(access -> {
            JsonNode node = access.jsonAt("/user/email");
            return node.asText();
        });
        assertEquals("alice@example.com", email);
        
        jsonRef.onRelease();
    }
    
    @Test
    void testFileRefValue() throws Exception {
        String testData = "This is test content for file storage";
        byte[] bytes = testData.getBytes();
        
        FileRefValue fileRef = FileRefValue.fromBytes(
            bytes, 
            "text/plain", 
            tempDir, 
            "test-"
        );
        
        assertEquals(RefValueType.FILE, fileRef.getType());
        assertEquals("text/plain", fileRef.getMediaType().orElse(null));
        assertEquals(bytes.length, fileRef.getSize());
        assertFalse(fileRef.isReleased());
        
        // Test streaming (preferred for binary/text files)
        try (var stream = fileRef.openStream()) {
            byte[] readBytes = stream.readAllBytes();
            assertArrayEquals(bytes, readBytes);
            String readContent = new String(readBytes);
            assertEquals(testData, readContent);
        }
        
        // Test cleanup
        Path filePath = fileRef.getFilePath();
        assertTrue(java.nio.file.Files.exists(filePath), "File should exist before release");
        
        fileRef.onRelease();
        assertTrue(fileRef.isReleased());
        assertFalse(java.nio.file.Files.exists(filePath), "File should be deleted after release");
    }
    
    @Test
    void testFileRefValueWithLargeJson() throws Exception {
        Map<String, Object> largeData = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeData.put("key" + i, "value" + i);
        }
        
        FileRefValue fileRef = FileRefValue.fromObject(
            largeData,
            tempDir,
            "large-json-"
        );
        
        assertEquals(RefValueType.FILE, fileRef.getType());
        assertTrue(fileRef.getSize() > 0);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> recovered = fileRef.read(Map.class);
        assertEquals(1000, recovered.size());
        assertEquals("value500", recovered.get("key500"));
        
        fileRef.onRelease();
    }
    
    @Test
    void testFileRefValueIdempotentRelease() throws Exception {
        byte[] data = "test".getBytes();
        FileRefValue fileRef = FileRefValue.fromBytes(data, "text/plain", tempDir, "test-");
        
        // First release
        fileRef.onRelease();
        assertTrue(fileRef.isReleased());
        
        // Second release should be safe (idempotent)
        assertDoesNotThrow(() -> fileRef.onRelease());
    }
    
    @Test
    void testDescriptorSerialization() {
        RefValueDescriptor descriptor = RefValueDescriptor.builder()
            .type(RefValueType.MEMORY)
            .mediaType("application/json")
            .size(1024)
            .inlineValue(Map.of("test", "value"))
            .build();
        
        assertEquals(RefValueType.MEMORY, descriptor.getType());
        assertEquals("application/json", descriptor.getMediaType());
        assertEquals(1024, descriptor.getSize());
        assertNotNull(descriptor.getInlineValue());
    }
}
