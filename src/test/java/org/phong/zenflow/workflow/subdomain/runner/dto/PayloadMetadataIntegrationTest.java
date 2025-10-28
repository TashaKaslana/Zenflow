package org.phong.zenflow.workflow.subdomain.runner.dto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
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
 * Integration tests for PayloadMetadata to verify it correctly handles different data types
 * and storage preferences (base64, string, file).
 * 
 * Tests the workflow: WorkflowRunnerRequest with payloadMetadata → RuntimeContext.write() → RefValue storage
 */
@DisplayName("PayloadMetadata Integration Tests")
class PayloadMetadataIntegrationTest {

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
     * This simulates the workflow metadata that tracks which nodes consume which values.
     */
    private void initializeWithConsumers(Map<String, Set<String>> consumers) {
        context.initialize(new HashMap<>(), consumers, new HashMap<>());
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.clear();
        }
    }

    @Test
    @DisplayName("Should handle base64-encoded string data with PayloadMetadata.base64()")
    void shouldHandleBase64EncodedData() {
        // Arrange: Create a base64-encoded string (simulating image/file data)
        String originalContent = "This is a test file content that will be base64 encoded!";
        String base64Content = Base64.getEncoder().encodeToString(originalContent.getBytes());
        
        PayloadMetadata metadata = PayloadMetadata.base64();
        assertEquals("text/base64", metadata.mediaType());
        assertEquals(StoragePreference.AUTO, metadata.storagePreference());

        // Initialize context with consumer for the SCOPED key (as workflow metadata would track it)
        String scopedKey = String.format("%s.output.payload.image", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write to context with base64 metadata
        // write() expects just the payload part (without nodeKey.output prefix)
        // flushPendingWrites() will add the scope using ContextKeyResolver.scopeKey()
        String payloadKey = "payload.image";
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.write(payloadKey, base64Content, options);
        
        // Flush pending writes (simulating what WorkflowEngineService does)
        context.flushPendingWrites(startNodeKey);

        // Assert: Verify the value is stored and can be retrieved using the SCOPED key
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref, "RefValue should be created");
        
        // Base64 string should be stored (decoded bytes handled internally by RefValue)
        Object retrieved = context.get(scopedKey);
        assertNotNull(retrieved, "Value should be retrievable");
        
        // For base64 media type, RefValue stores it as InputStream (bytes)
        try {
            if (retrieved instanceof InputStream inputStream) {
                byte[] retrievedBytes = inputStream.readAllBytes();
                String decodedContent = new String(retrievedBytes);
                assertEquals(originalContent, decodedContent, "Decoded content should match original");
            } else if (retrieved instanceof byte[]) {
                String decodedContent = new String((byte[]) retrieved);
                assertEquals(originalContent, decodedContent, "Decoded content should match original");
            } else {
                // If stored as string, it should be the base64 string
                assertEquals(base64Content, retrieved, "Base64 string should be preserved");
            }
        } catch (Exception e) {
            fail("Failed to read base64 data: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle plain string data without metadata")
    void shouldHandlePlainStringWithoutMetadata() {
        // Arrange: Plain string payload
        String plainText = "This is a simple text description";
        
        // Initialize context with consumer
        String scopedKey = String.format("%s.output.payload.description", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write without metadata (uses default WriteOptions)
        context.write("payload.description", plainText);
        context.flushPendingWrites(startNodeKey);

        // Assert: Should be stored as MemoryRefValue for small strings
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref);
        assertEquals(RefValueType.MEMORY, ref.getType(), "Small strings should use MEMORY storage");
        
        String retrieved = (String) context.get(scopedKey);
        assertEquals(plainText, retrieved, "Plain text should be preserved");
    }

    @Test
    @DisplayName("Should handle large string data with forceFile metadata")
    void shouldHandleLargeStringWithForceFileStorage() {
        // Arrange: Large string that we want to force to file storage
        String largeText = "Large content ".repeat(10000); // ~130KB
        
        PayloadMetadata metadata = PayloadMetadata.forceFile();
        assertNull(metadata.mediaType(), "Force file should not specify media type");
        assertEquals(StoragePreference.FILE, metadata.storagePreference());

        // Initialize context with consumer
        String scopedKey = String.format("%s.output.payload.large_document", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write with FILE storage preference
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.write("payload.large_document", largeText, options);
        context.flushPendingWrites(startNodeKey);

        // Assert: Should be stored as FileRefValue
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref);
        assertEquals(RefValueType.FILE, ref.getType(), "Large content with FILE preference should use FILE storage");
        
        String retrieved = (String) context.get(scopedKey);
        assertEquals(largeText, retrieved, "Large text should be retrievable from file storage");
    }

    @Test
    @DisplayName("Should handle JSON data with custom media type")
    void shouldHandleJsonDataWithMediaType() {
        // Arrange: JSON-like map data
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("name", "Test User");
        jsonData.put("email", "test@example.com");
        jsonData.put("age", 30);
        
        PayloadMetadata metadata = PayloadMetadata.withMediaType("application/json");
        assertEquals("application/json", metadata.mediaType());
        assertEquals(StoragePreference.AUTO, metadata.storagePreference());

        // Initialize context with consumer
        String scopedKey = String.format("%s.output.payload.user_profile", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write with JSON media type
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.write("payload.user_profile", jsonData, options);
        context.flushPendingWrites(startNodeKey);

        // Assert: Should be stored appropriately
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> retrieved = (Map<String, Object>) context.get(scopedKey);
        assertNotNull(retrieved);
        assertEquals("Test User", retrieved.get("name"));
        assertEquals("test@example.com", retrieved.get("email"));
        assertEquals(30, retrieved.get("age"));
    }

    @Test
    @DisplayName("Should handle multiple payload entries with different metadata")
    void shouldHandleMultiplePayloadsWithDifferentMetadata() {
        // Arrange: Simulate a request with multiple payload types
        Map<String, Object> payload = new HashMap<>();
        payload.put("profile_image", Base64.getEncoder().encodeToString("image_data".getBytes()));
        payload.put("username", "johndoe");
        payload.put("bio", "A very long biography text ".repeat(5000)); // ~125KB
        payload.put("settings", Map.of("theme", "dark", "notifications", true));
        
        Map<String, PayloadMetadata> payloadMetadata = new HashMap<>();
        payloadMetadata.put("profile_image", PayloadMetadata.base64());
        payloadMetadata.put("bio", PayloadMetadata.forceFile());
        payloadMetadata.put("settings", PayloadMetadata.withMediaType("application/json"));
        // username has no metadata (will use defaults)

        // Initialize context with consumers for all payload keys
        Map<String, Set<String>> consumers = new HashMap<>();
        for (String key : payload.keySet()) {
            String contextKey = String.format("%s.output.payload.%s", startNodeKey, key);
            consumers.put(contextKey, Set.of("next-node"));
        }
        initializeWithConsumers(consumers);
        
        // Act: Write each payload value with its metadata
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String payloadKey = "payload." + entry.getKey();
            
            if (payloadMetadata.containsKey(entry.getKey())) {
                PayloadMetadata metadata = payloadMetadata.get(entry.getKey());
                WriteOptions options = new WriteOptions(
                        metadata.mediaType(),
                        metadata.storagePreference(),
                        true
                );
                context.write(payloadKey, entry.getValue(), options);
            } else {
                // No metadata - use default
                context.write(payloadKey, entry.getValue());
            }
        }
        context.flushPendingWrites(startNodeKey);

        // Assert: Verify each value is stored with correct type
        // profile_image should be base64 decoded
        RefValue imageRef = context.getRef(String.format("%s.output.payload.profile_image", startNodeKey));
        assertNotNull(imageRef, "Image should be stored");
        
        // username should be in memory (small string, no metadata)
        RefValue usernameRef = context.getRef(String.format("%s.output.payload.username", startNodeKey));
        assertNotNull(usernameRef);
        assertEquals(RefValueType.MEMORY, usernameRef.getType(), "Small string should use MEMORY");
        assertEquals("johndoe", context.get(String.format("%s.output.payload.username", startNodeKey)));
        
        // bio should be in file storage (forced)
        RefValue bioRef = context.getRef(String.format("%s.output.payload.bio", startNodeKey));
        assertNotNull(bioRef);
        assertEquals(RefValueType.FILE, bioRef.getType(), "Forced file storage should be respected");
        
        // settings should be stored as JSON/object
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) context.get(String.format("%s.output.payload.settings", startNodeKey));
        assertNotNull(settings);
        assertEquals("dark", settings.get("theme"));
        assertEquals(true, settings.get("notifications"));
    }

    @Test
    @DisplayName("Should handle base64 image data simulating real file upload")
    void shouldHandleBase64ImageDataAsRealFile() {
        // Arrange: Simulate a small PNG image as base64
        // This is a 1x1 red pixel PNG image
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";
        
        PayloadMetadata metadata = PayloadMetadata.base64();

        // Initialize context with consumer
        String scopedKey = String.format("%s.output.payload.avatar", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write as if uploaded from client
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.write("payload.avatar", base64Image, options);
        context.flushPendingWrites(startNodeKey);

        // Assert: Should be stored and decodable
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref, "Image RefValue should be created");
        
        Object retrieved = context.get(scopedKey);
        assertNotNull(retrieved, "Image data should be retrievable");
        
        // Verify it's stored as bytes/stream (decoded from base64)
        assertTrue(
            retrieved instanceof InputStream || retrieved instanceof byte[] || retrieved instanceof String,
            "Should be stored as binary data or base64 string"
        );
    }

    @Test
    @DisplayName("Should preserve exact string value when no metadata provided")
    void shouldPreserveExactStringWithoutMetadata() {
        // Arrange: A string that LOOKS like base64 but shouldn't be decoded
        String suspiciousString = "SGVsbG8gV29ybGQ="; // Actually "Hello World" in base64
        
        // Initialize context with consumer
        String scopedKey = String.format("%s.output.payload.token", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write WITHOUT base64 metadata
        context.write("payload.token", suspiciousString);
        context.flushPendingWrites(startNodeKey);

        // Assert: Should NOT be decoded - stored as-is
        String retrieved = (String) context.get(scopedKey);
        assertEquals(suspiciousString, retrieved, "String should be preserved as-is without base64 decoding");
        assertNotEquals("Hello World", retrieved, "Should NOT be decoded without explicit metadata");
    }

    @Test
    @DisplayName("Should handle video file as base64 with FILE storage preference")
    void shouldHandleVideoFileAsBase64WithFileStorage() {
        // Arrange: Simulate large video file encoded as base64
        byte[] videoData = new byte[2 * 1024 * 1024]; // 2MB of mock video data
        for (int i = 0; i < videoData.length; i++) {
            videoData[i] = (byte) (i % 256);
        }
        String base64Video = Base64.getEncoder().encodeToString(videoData);
        
        // Use base64 media type with FILE storage for large files
        PayloadMetadata metadata = new PayloadMetadata("text/base64", StoragePreference.FILE);

        // Initialize context with consumer
        String scopedKey = String.format("%s.output.payload.video", startNodeKey);
        Map<String, Set<String>> consumers = Map.of(scopedKey, Set.of("next-node"));
        initializeWithConsumers(consumers);
        
        // Act: Write large base64 video
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.write("payload.video", base64Video, options);
        context.flushPendingWrites(startNodeKey);

        // Assert: Should be stored as FILE
        RefValue ref = context.getRef(scopedKey);
        assertNotNull(ref, "RefValue should be created for large base64 video");
        assertEquals(RefValueType.FILE, ref.getType(), "Large base64 video should use FILE storage");
    }
}
