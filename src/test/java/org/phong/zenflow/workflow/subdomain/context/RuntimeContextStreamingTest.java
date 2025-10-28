package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.context.refvalue.RefValue;
import org.phong.zenflow.workflow.subdomain.context.refvalue.RuntimeContextRefValueSupport;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.RefValueType;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.StoragePreference;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.WriteOptions;

import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RuntimeContext streaming APIs (openStream/writeStream).
 * <p>
 * Tests the progressive streaming capabilities for efficient handling of large
 * binary payloads without loading entire content into memory.
 */
@DisplayName("RuntimeContext Streaming Tests")
class RuntimeContextStreamingTest {

    private RuntimeContextRefValueSupport support;
    private RuntimeContext context;
    private String startNodeKey;

    @BeforeEach
    void setUp() {
        support = new RuntimeContextRefValueSupport();
        context = new RuntimeContext(support);
        startNodeKey = "start";
    }
    
    /**
     * Helper method to initialize context with consumers for specific keys.
     */
    private void initializeWithConsumers(Map<String, Set<String>> consumers) {
        context.initialize(new HashMap<>(), consumers, new HashMap<>());
    }

    @AfterEach
    void tearDown() {
        context.clear();
    }

    @Test
    @DisplayName("Should stream large file data without loading into memory using openStream()")
    void shouldStreamLargeFileDataWithoutMaterialization() throws Exception {
        // Arrange: Create large binary data
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        String base64Data = Base64.getEncoder().encodeToString(largeData);
        
        // Initialize context with consumer
        String scopedKey = String.format("%s.output.data.large_file", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write large file with base64 metadata
        WriteOptions options = new WriteOptions("text/base64", StoragePreference.FILE, true);
        context.write("data.large_file", base64Data, options);
        context.flushPendingWrites(startNodeKey);
        
        // Assert: Can stream the data without materializing into memory
        try (InputStream stream = context.openStream(scopedKey)) {
            assertNotNull(stream, "Stream should be opened successfully");
            
            // Read first few bytes to verify stream works
            byte[] buffer = new byte[100];
            int bytesRead = stream.read(buffer);
            assertTrue(bytesRead > 0, "Should be able to read bytes from stream");
            
            // Verify the streamed data matches original (first 100 bytes of decoded content)
            for (int i = 0; i < 100 && i < bytesRead; i++) {
                assertEquals(largeData[i], buffer[i], "Streamed data should match original at index " + i);
            }
        }
        
        // Verify we can open multiple streams independently
        try (InputStream stream1 = context.openStream(scopedKey);
             InputStream stream2 = context.openStream(scopedKey)) {
            assertNotNull(stream1, "First stream should open");
            assertNotNull(stream2, "Second stream should open independently");
            
            // Both streams should be readable
            assertTrue(stream1.read() >= 0, "First stream should be readable");
            assertTrue(stream2.read() >= 0, "Second stream should be readable");
        }
    }
    
    @Test
    @DisplayName("Should write large binary data using writeStream() for efficient streaming")
    void shouldWriteLargeBinaryDataUsingWriteStream() throws Exception {
        // Arrange: Create a 2MB binary data stream
        byte[] largeData = new byte[2 * 1024 * 1024]; // 2MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        // Initialize context with consumer
        String scopedKey = String.format("%s.output.data.video_stream", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write using stream API (more efficient than passing byte[] directly)
        WriteOptions options = new WriteOptions("video/mp4", StoragePreference.FILE, true);
        try (InputStream inputStream = new java.io.ByteArrayInputStream(largeData)) {
            context.writeStream("data.video_stream", inputStream, options);
        }
        context.flushPendingWrites(startNodeKey);
        
        // Assert: Verify the data was stored correctly
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref, "RefValue should be created");
        assertEquals(RefValueType.FILE, ref.getType(), "Large stream should use FILE storage");
        
        // Verify we can read it back via stream
        try (InputStream outputStream = context.openStream(scopedKey)) {
            byte[] readData = outputStream.readAllBytes();
            assertEquals(largeData.length, readData.length, "Read data length should match original");
            assertArrayEquals(largeData, readData, "Read data should match original byte-for-byte");
        }
    }
    
    @Test
    @DisplayName("Should handle writeStream() with raw binary data correctly")
    void shouldHandleWriteStreamWithRawBinaryData() throws Exception {
        // Arrange: Create binary data with mixed bytes
        byte[] binaryData = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x48, 0x65, 0x6C, 0x6C, 0x6F}; // Some binary + "Hello"
        
        // Initialize context with consumer
        String scopedKey = String.format("%s.output.data.binary_stream", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write using stream API with binary media type
        WriteOptions options = new WriteOptions("application/octet-stream", StoragePreference.AUTO, true);
        try (InputStream inputStream = new java.io.ByteArrayInputStream(binaryData)) {
            context.writeStream("data.binary_stream", inputStream, options);
        }
        context.flushPendingWrites(startNodeKey);
        
        // Assert: Verify the raw bytes are preserved exactly
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref, "RefValue should be created");
        
        // openStream() should return the RAW bytes, not JSON-serialized/base64-encoded
        // MemoryRefValue.openStream() handles byte[] specially to return raw bytes
        try (InputStream outputStream = context.openStream(scopedKey)) {
            byte[] readData = outputStream.readAllBytes();
            assertArrayEquals(binaryData, readData, "Read binary data should match original byte-for-byte");
        }
    }
    
    @Test
    @DisplayName("Should overwrite previous value when writing to same key (not append)")
    void shouldOverwritePreviousValueNotAppend() throws Exception {
        // Arrange
        String scopedKey = String.format("%s.output.data.overwrite", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write twice to the same key
        byte[] firstWrite = "First value".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] secondWrite = "Second value".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        WriteOptions options = new WriteOptions("text/plain", StoragePreference.AUTO, true);
        try (InputStream stream1 = new java.io.ByteArrayInputStream(firstWrite)) {
            context.writeStream("data.overwrite", stream1, options);
        }
        try (InputStream stream2 = new java.io.ByteArrayInputStream(secondWrite)) {
            context.writeStream("data.overwrite", stream2, options);
        }
        context.flushPendingWrites(startNodeKey);
        
        // Assert: Should only have the second value (overwrite behavior)
        try (InputStream outputStream = context.openStream(scopedKey)) {
            String readText = new String(outputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals("Second value", readText, "Second write should overwrite first (standard map behavior)");
        }
    }
    
    @Test
    @DisplayName("Should stream very large data progressively using FILE storage without loading into memory")
    void shouldStreamVeryLargeDataProgressively() throws Exception {
        // Arrange: Create a 5MB stream (large enough to trigger FILE storage)
        int size = 5 * 1024 * 1024; // 5MB
        
        String scopedKey = String.format("%s.output.data.large_video", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write using streaming API - this should use FileRefValue.fromStream()
        // which uses Files.copy() for progressive streaming without loading into memory
        WriteOptions options = new WriteOptions("video/mp4", StoragePreference.AUTO, true);
        
        // Create a custom InputStream that generates data on-the-fly
        InputStream largeStream = new InputStream() {
            private int remaining = size;
            
            @Override
            public int read() {
                if (remaining <= 0) return -1;
                remaining--;
                return (byte) (remaining % 256);
            }
            
            @Override
            public int read(byte[] b, int off, int len) {
                if (remaining <= 0) return -1;
                int toRead = Math.min(len, remaining);
                for (int i = 0; i < toRead; i++) {
                    b[off + i] = (byte) ((remaining - i) % 256);
                }
                remaining -= toRead;
                return toRead;
            }
        };
        
        context.writeStream("data.large_video", largeStream, options);
        context.flushPendingWrites(startNodeKey);
        
        // Assert: Should use FILE storage for large data
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref, "RefValue should be created");
        assertEquals(RefValueType.FILE, ref.getType(), 
            "Large streamed data (5MB) should use FILE storage for memory efficiency");
        
        // Verify we can stream it back
        try (InputStream readStream = context.openStream(scopedKey)) {
            assertNotNull(readStream, "Should be able to open stream for reading");
            
            // Read first 1000 bytes to verify content (don't read all 5MB in test)
            byte[] sample = new byte[1000];
            int bytesRead = readStream.read(sample);
            assertTrue(bytesRead > 0, "Should be able to read from stream");
        }
    }
}
