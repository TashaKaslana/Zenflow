package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.WriteOptions;
import org.phong.zenflow.workflow.subdomain.runner.dto.PayloadMetadata;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RuntimeContextManager Cleanup Tests")
class RuntimeContextManagerCleanupTest {

    private RuntimeContextManager contextManager;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Path.of("./data/context-files");
        Files.createDirectories(tempDir);
        
        contextManager = new RuntimeContextManager();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Additional cleanup if needed
        if (Files.exists(tempDir)) {
            Files.list(tempDir).forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            });
        }
    }

    @Test
    @DisplayName("Should cleanup RefValue temp files when context is removed via remove()")
    void shouldCleanupRefValueFilesOnRemove() throws Exception {
        // Given: Context with file-backed RefValue
        String workflowRunId = "test-workflow-run-1";
        RuntimeContext context = contextManager.getOrCreate(workflowRunId);
        
        // Initialize with consumers so values are actually stored
        context.initialize(Map.of(), Map.of("test-node.output.largeData", Set.of("consumer")), Map.of());
        
        // Create large data that will be stored in file (> 1MB)
        byte[] largeData = new byte[2 * 1024 * 1024]; // 2MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        // Write via streaming API (forces file storage)
        ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData);
        PayloadMetadata metadata = PayloadMetadata.forceFile();
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.writeStream("largeData", inputStream, options);
        context.flushPendingWrites("test-node");
        
        // Verify file was created
        long fileCountBefore = Files.list(tempDir).count();
        assertTrue(fileCountBefore > 0, "Expected temp file to be created");
        
        // When: Remove context from manager
        contextManager.remove(workflowRunId);
        
        // Then: Temp file should be deleted
        long fileCountAfter = Files.list(tempDir).count();
        assertEquals(0, fileCountAfter, "Expected all temp files to be cleaned up");
    }

    @Test
    @DisplayName("Should cleanup RefValue temp files when context is invalidated")
    void shouldCleanupRefValueFilesOnInvalidate() throws Exception {
        // Given: Context with file-backed RefValue
        String workflowRunId = "test-workflow-run-2";
        RuntimeContext context = contextManager.getOrCreate(workflowRunId);
        
        // Initialize with consumers so values are actually stored
        context.initialize(Map.of(), Map.of("test-node.output.largeData", Set.of("consumer")), Map.of());
        
        // Create large data
        byte[] largeData = new byte[2 * 1024 * 1024]; // 2MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData);
        PayloadMetadata metadata = PayloadMetadata.forceFile();
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.writeStream("largeData", inputStream, options);
        context.flushPendingWrites("test-node");
        
        long fileCountBefore = Files.list(tempDir).count();
        assertTrue(fileCountBefore > 0, "Expected temp file to be created");
        
        // When: Invalidate context (triggers removalListener)
        contextManager.invalidate(workflowRunId);
        
        // Then: Temp file should be deleted
        long fileCountAfter = Files.list(tempDir).count();
        assertEquals(0, fileCountAfter, "Expected all temp files to be cleaned up");
    }

    @Test
    @DisplayName("Should cleanup multiple contexts independently")
    void shouldCleanupMultipleContextsIndependently() throws Exception {
        // Given: Two contexts with file-backed RefValues
        String workflowRunId1 = "test-workflow-run-3";
        String workflowRunId2 = "test-workflow-run-4";
        
        RuntimeContext context1 = contextManager.getOrCreate(workflowRunId1);
        RuntimeContext context2 = contextManager.getOrCreate(workflowRunId2);
        
        // Initialize both contexts with consumers
        context1.initialize(Map.of(), Map.of("node1.output.data1", Set.of("consumer")), Map.of());
        context2.initialize(Map.of(), Map.of("node2.output.data2", Set.of("consumer")), Map.of());
        
        byte[] largeData = new byte[2 * 1024 * 1024]; // 2MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        // Write to both contexts
        PayloadMetadata metadata = PayloadMetadata.forceFile();
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context1.writeStream("data1", new ByteArrayInputStream(largeData), options);
        context1.flushPendingWrites("node1");
        
        context2.writeStream("data2", new ByteArrayInputStream(largeData), options);
        context2.flushPendingWrites("node2");
        
        long fileCountBefore = Files.list(tempDir).count();
        assertEquals(2, fileCountBefore, "Expected 2 temp files to be created");
        
        // When: Remove first context only
        contextManager.remove(workflowRunId1);
        
        // Then: Only first context's file should be deleted
        long fileCountAfterFirst = Files.list(tempDir).count();
        assertEquals(1, fileCountAfterFirst, "Expected 1 temp file to remain");
        
        // When: Remove second context
        contextManager.remove(workflowRunId2);
        
        // Then: All files should be deleted
        long fileCountAfterSecond = Files.list(tempDir).count();
        assertEquals(0, fileCountAfterSecond, "Expected all temp files to be cleaned up");
    }

    @Test
    @DisplayName("Should handle remove() when context doesn't exist")
    void shouldHandleRemoveWhenContextDoesNotExist() {
        // When: Remove non-existent context
        RuntimeContext removed = contextManager.remove("non-existent-id");
        
        // Then: Should return null without error
        assertNull(removed, "Expected null for non-existent context");
    }

    @Test
    @DisplayName("Should handle remove() when context has no RefValues")
    void shouldHandleRemoveWhenContextHasNoRefValues() {
        // Given: Context with simple values (no RefValues)
        String workflowRunId = "test-workflow-run-5";
        RuntimeContext context = contextManager.getOrCreate(workflowRunId);
        context.write("simpleKey", "simpleValue", WriteOptions.DEFAULT);
        context.flushPendingWrites("test-node");
        
        // When: Remove context
        RuntimeContext removed = contextManager.remove(workflowRunId);
        
        // Then: Should succeed without error
        assertNotNull(removed, "Expected context to be removed successfully");
    }

    @Test
    @DisplayName("Should cleanup pending writes with RefValues when context is cleared before flush")
    void shouldCleanupPendingWritesOnClearBeforeFlush() throws Exception {
        // Given: Context with pending write containing RefValue (simulating node failure)
        String workflowRunId = "test-workflow-run-6";
        RuntimeContext context = contextManager.getOrCreate(workflowRunId);
        
        // Initialize with consumers
        context.initialize(Map.of(), Map.of("test-node.output.largeData", Set.of("consumer")), Map.of());
        
        // Create large data that will be stored in file
        byte[] largeData = new byte[2 * 1024 * 1024]; // 2MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        
        // Write via streaming API but DON'T flush (simulating failure before flush)
        ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData);
        PayloadMetadata metadata = PayloadMetadata.forceFile();
        WriteOptions options = new WriteOptions(
                metadata.mediaType(),
                metadata.storagePreference(),
                true
        );
        context.writeStream("largeData", inputStream, options);
        // NOTE: NOT calling flushPendingWrites() - simulating node failure
        
        // Verify file was created for pending write
        long fileCountBefore = Files.list(tempDir).count();
        assertTrue(fileCountBefore > 0, "Expected temp file to be created for pending write");
        
        // When: Clear context without flushing (simulating error handling)
        contextManager.remove(workflowRunId);
        
        // Then: Pending write RefValue should be released
        long fileCountAfter = Files.list(tempDir).count();
        assertEquals(0, fileCountAfter, "Expected pending write RefValue to be cleaned up");
    }
}
