package org.phong.zenflow.workflow.subdomain.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phong.zenflow.workflow.subdomain.logging.buffer.WorkflowBufferManager;
import org.phong.zenflow.workflow.subdomain.logging.collector.GlobalLogCollector;
import org.phong.zenflow.workflow.subdomain.logging.config.LoggingProperties;
import org.phong.zenflow.workflow.subdomain.logging.core.*;
import org.phong.zenflow.workflow.subdomain.logging.util.SharedThreadPoolManager;
import org.springframework.context.ApplicationEventPublisher;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LoggingNodeTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GlobalLogCollector globalLogCollector;

    @Mock
    private SharedThreadPoolManager threadPoolManager;

    private WorkflowBufferManager bufferManager;
    private LoggingProperties.BufferConfig bufferConfig;

    // Test workflow and node setup
    private final UUID workflowId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String[] nodeKeys = {"DataProcessor", "ValidationEngine", "OutputHandler"};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup buffer configuration
        bufferConfig = new LoggingProperties.BufferConfig();
        bufferConfig.setDefaultBatchSize(5);
        bufferConfig.setMaxBatchSize(20);
        bufferConfig.setMinBatchSize(1);
        bufferConfig.setMaxDelayMs(1000);
        bufferConfig.setRingBufferSize(10);
        bufferConfig.setAdaptiveBatching(true);
        bufferConfig.setCleanupIdleAfterMs(30000);

        // Mock thread pool manager
        when(threadPoolManager.getBatchProcessorService()).thenReturn(Executors.newCachedThreadPool());
        when(threadPoolManager.getSharedScheduler()).thenReturn(Executors.newScheduledThreadPool(2));
        when(threadPoolManager.getActiveWorkflowCount()).thenReturn(1);

        // Create buffer manager
        bufferManager = new WorkflowBufferManager(globalLogCollector, bufferConfig, threadPoolManager);
    }

    @Test
    void testThreeNodeWorkflowWithContextIsolation() throws InterruptedException, ExecutionException, TimeoutException {
        // Test that logs from 3 different nodes maintain proper context isolation

        // Create 3 separate workflow runs to simulate parallel execution
        UUID[] runIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        String[] traceIds = {"trace-workflow-1", "trace-workflow-2", "trace-workflow-3"};

        List<LogEntry> capturedLogs = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(9); // 3 logs per workflow

        // Capture logs from GlobalLogCollector
        doAnswer(invocation -> {
            List<LogEntry> entries = invocation.getArgument(1);
            synchronized (capturedLogs) {
                capturedLogs.addAll(entries);
                entries.forEach(e -> latch.countDown());
            }
            return null;
        }).when(globalLogCollector).accept(any(), any());

        // Execute 3 workflows concurrently, each with 3 nodes
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Void>> futures = new ArrayList<>();

        for (int workflowIndex = 0; workflowIndex < 3; workflowIndex++) {
            final int index = workflowIndex;
            futures.add(executor.submit(() -> {
                executeWorkflowWithThreeNodes(runIds[index], traceIds[index], index);
                return null;
            }));
        }

        // Wait for all workflows to complete
        for (Future<Void> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }

        // Wait for all logs to be processed
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All logs should be processed within timeout");

        // Verify we have logs from all 3 workflows
        assertEquals(9, capturedLogs.size(), "Should have 9 total log entries (3 workflows × 3 nodes)");

        // Verify context isolation - each workflow should have its own trace ID
        Map<String, List<LogEntry>> logsByTrace = new HashMap<>();
        capturedLogs.forEach(log ->
            logsByTrace.computeIfAbsent(log.getTraceId(), k -> new ArrayList<>()).add(log)
        );

        assertEquals(3, logsByTrace.size(), "Should have logs from 3 different trace contexts");

        // Verify each workflow has logs from all 3 nodes
        for (int i = 0; i < 3; i++) {
            String expectedTrace = traceIds[i];
            List<LogEntry> workflowLogs = logsByTrace.get(expectedTrace);
            assertNotNull(workflowLogs, "Should have logs for trace: " + expectedTrace);
            assertEquals(3, workflowLogs.size(), "Each workflow should have 3 log entries");

            // Verify node hierarchy for this workflow
            Set<String> nodeKeysInWorkflow = new HashSet<>();
            workflowLogs.forEach(log -> nodeKeysInWorkflow.add(log.getNodeKey()));

            assertEquals(3, nodeKeysInWorkflow.size(), "Should have logs from 3 different nodes");
            assertTrue(nodeKeysInWorkflow.containsAll(Arrays.asList(nodeKeys)),
                "Should contain all expected node keys");
        }

        executor.shutdown();
    }

    @Test
    void testLogLevelPriorityAndBatching() throws InterruptedException {
        // Test that ERROR logs get immediate processing while others follow batching rules

        UUID runId = UUID.randomUUID();
        String traceId = "priority-test-trace";

        List<LogEntry> capturedLogs = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch errorLatch = new CountDownLatch(1);
        CountDownLatch allLogsLatch = new CountDownLatch(6); // 2 logs per node

        doAnswer(invocation -> {
            List<LogEntry> entries = invocation.getArgument(1);
            capturedLogs.addAll(entries);

            // Check if any ERROR logs are present
            boolean hasError = entries.stream().anyMatch(log -> log.getLevel() == LogLevel.ERROR);
            if (hasError) {
                errorLatch.countDown();
            }

            entries.forEach(e -> allLogsLatch.countDown());
            return null;
        }).when(globalLogCollector).accept(any(), any());

        // Initialize context for single workflow
        LogContextManager.init(runId.toString(), traceId);

        try {
            // Create node publishers
            NodeLogPublisher[] publishers = new NodeLogPublisher[3];
            for (int i = 0; i < 3; i++) {
                publishers[i] = NodeLogPublisher.builder()
                    .publisher(eventPublisher)
                    .workflowId(workflowId)
                    .runId(runId)
                    .nodeKey(nodeKeys[i])
                    .userId(userId)
                    .build();
            }

            // Log INFO messages from all nodes
            for (int i = 0; i < 3; i++) {
                LogContextManager.push(nodeKeys[i]);
                publishers[i].info("Processing data in node {}", nodeKeys[i]);
                LogContextManager.pop();
            }

            // Log ERROR from middle node - should trigger immediate processing
            LogContextManager.push(nodeKeys[1]);
            publishers[1].error("Critical error in validation").withErrorCode("VALIDATION_FAILED").log();
            LogContextManager.pop();

            // Wait for ERROR log to be processed immediately
            assertTrue(errorLatch.await(2, TimeUnit.SECONDS),
                "ERROR log should be processed immediately");

            // Log more INFO messages
            for (int i = 0; i < 3; i++) {
                LogContextManager.push(nodeKeys[i]);
                publishers[i].info("Completing processing in node {}", nodeKeys[i]);
                LogContextManager.pop();
            }

            // Wait for all logs
            assertTrue(allLogsLatch.await(5, TimeUnit.SECONDS),
                "All logs should be processed within timeout");

        } finally {
            LogContextManager.cleanup(runId.toString());
        }

        // Verify log content and priority handling
        assertEquals(6, capturedLogs.size(), "Should have 6 total log entries");

        // Find the ERROR log
        Optional<LogEntry> errorLog = capturedLogs.stream()
            .filter(log -> log.getLevel() == LogLevel.ERROR)
            .findFirst();

        assertTrue(errorLog.isPresent(), "Should have one ERROR log");
        assertEquals("VALIDATION_FAILED", errorLog.get().getErrorCode());
        assertEquals(nodeKeys[1], errorLog.get().getNodeKey());
        assertEquals(traceId, errorLog.get().getTraceId());
    }

    @Test
    void testLogContextHierarchyTracking() throws InterruptedException {
        // Test that the hierarchy is properly tracked through nested component calls

        UUID runId = UUID.randomUUID();
        String traceId = "hierarchy-test-trace";

        List<LogEntry> capturedLogs = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(6); // 2 logs per node level

        doAnswer(invocation -> {
            List<LogEntry> entries = invocation.getArgument(1);
            capturedLogs.addAll(entries);
            entries.forEach(e -> latch.countDown());
            return null;
        }).when(globalLogCollector).accept(any(), any());

        // Initialize context
        LogContextManager.init(runId.toString(), traceId);

        try {
            NodeLogPublisher publisher = NodeLogPublisher.builder()
                .publisher(eventPublisher)
                .workflowId(workflowId)
                .runId(runId)
                .nodeKey("HierarchyTestNode")
                .userId(userId)
                .build();

            // Simulate nested component execution with hierarchy tracking
            LogContextManager.push("WorkflowEngine");
            publisher.info("Starting workflow execution");

            LogContextManager.push("DataProcessor");
            publisher.info("Processing input data");

            LogContextManager.push("ValidationModule");
            publisher.warn("Validation warning detected");

            // Test withComponent helper
            LogContextManager.withComponent("DatabaseConnector", () -> {
                publisher.debug("Connecting to database");
                publisher.success("Database connection established");
                return null;
            });

            publisher.info("Validation completed");
            LogContextManager.pop(); // Remove ValidationModule

            LogContextManager.pop(); // Remove DataProcessor
            LogContextManager.pop(); // Remove WorkflowEngine

        } finally {
            LogContextManager.cleanup(runId.toString());
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All logs should be processed");
        assertEquals(6, capturedLogs.size(), "Should have 6 log entries");

        // Verify hierarchy progression
        List<String> expectedHierarchies = Arrays.asList(
            "WorkflowEngine",
            "WorkflowEngine->DataProcessor",
            "WorkflowEngine->DataProcessor->ValidationModule",
            "WorkflowEngine->DataProcessor->ValidationModule->DatabaseConnector",
            "WorkflowEngine->DataProcessor->ValidationModule->DatabaseConnector",
            "WorkflowEngine->DataProcessor->ValidationModule"
        );

        // Sort logs by timestamp to verify hierarchy progression
        capturedLogs.sort(Comparator.comparing(LogEntry::getTimestamp));

        for (int i = 0; i < Math.min(capturedLogs.size(), expectedHierarchies.size()); i++) {
            LogEntry log = capturedLogs.get(i);
            String expectedHierarchy = expectedHierarchies.get(i);
            assertEquals(expectedHierarchy, log.getHierarchy(),
                "Log " + i + " should have hierarchy: " + expectedHierarchy);
            assertEquals(traceId, log.getTraceId(), "All logs should have same trace ID");
        }
    }

    @Test
    void testConcurrentWorkflowExecution() throws InterruptedException, ExecutionException, TimeoutException {
        // Test multiple workflows executing concurrently without context pollution

        int numWorkflows = 5;
        int logsPerWorkflow = 3; // One per node
        CountDownLatch latch = new CountDownLatch(numWorkflows * logsPerWorkflow);

        List<LogEntry> capturedLogs = Collections.synchronizedList(new ArrayList<>());

        doAnswer(invocation -> {
            List<LogEntry> entries = invocation.getArgument(1);
            capturedLogs.addAll(entries);
            entries.forEach(e -> latch.countDown());
            return null;
        }).when(globalLogCollector).accept(any(), any());

        ExecutorService executor = Executors.newFixedThreadPool(numWorkflows);
        List<Future<Void>> futures = new ArrayList<>();

        // Launch multiple concurrent workflows
        for (int i = 0; i < numWorkflows; i++) {
            final int workflowIndex = i;
            futures.add(executor.submit(() -> {
                UUID runId = UUID.randomUUID();
                String traceId = "concurrent-trace-" + workflowIndex;

                LogContextManager.init(runId.toString(), traceId);
                try {
                    executeWorkflowWithThreeNodes(runId, traceId, workflowIndex);
                } finally {
                    LogContextManager.cleanup(runId.toString());
                }
                return null;
            }));
        }

        // Wait for completion
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All concurrent logs should be processed");
        assertEquals(numWorkflows * logsPerWorkflow, capturedLogs.size());

        // Verify no context pollution - each trace should have exactly 3 logs
        Map<String, Long> logCountByTrace = capturedLogs.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                LogEntry::getTraceId,
                java.util.stream.Collectors.counting()
            ));

        assertEquals(numWorkflows, logCountByTrace.size(), "Should have logs from all workflows");
        logCountByTrace.values().forEach(count ->
            assertEquals(3L, count.longValue(), "Each workflow should have exactly 3 logs"));

        executor.shutdown();
    }

    private void executeWorkflowWithThreeNodes(UUID runId, String traceId, int workflowIndex) {
        // Simulate execution of workflow with 3 nodes

        NodeLogPublisher[] publishers = new NodeLogPublisher[3];
        for (int i = 0; i < 3; i++) {
            publishers[i] = NodeLogPublisher.builder()
                .publisher(eventPublisher)
                .workflowId(workflowId)
                .runId(runId)
                .nodeKey(nodeKeys[i])
                .userId(userId)
                .build();
        }

        // Execute each node with proper context management
        for (int nodeIndex = 0; nodeIndex < 3; nodeIndex++) {
            LogContextManager.push(nodeKeys[nodeIndex]);
            try {
                String message = String.format("Workflow %d executing %s",
                    workflowIndex, nodeKeys[nodeIndex]);

                switch (nodeIndex) {
                    case 0 -> publishers[nodeIndex].info(message);
                    case 1 -> publishers[nodeIndex].debug(message);
                    case 2 -> publishers[nodeIndex].success(message);
                }

                // Simulate some processing time
                Thread.sleep(10);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                LogContextManager.pop();
            }
        }
    }

    @Test
    void testLogMetadataAndCorrelation() {
        // Test that log entries contain proper metadata and can be correlated

        UUID runId = UUID.randomUUID();
        String traceId = "metadata-test-trace";

        List<LogEntry> capturedLogs = Collections.synchronizedList(new ArrayList<>());

        doAnswer(invocation -> {
            List<LogEntry> entries = invocation.getArgument(1);
            capturedLogs.addAll(entries);
            return null;
        }).when(globalLogCollector).accept(any(), any());

        LogContextManager.init(runId.toString(), traceId);

        try {
            NodeLogPublisher publisher = NodeLogPublisher.builder()
                .publisher(eventPublisher)
                .workflowId(workflowId)
                .runId(runId)
                .nodeKey("MetadataTestNode")
                .userId(userId)
                .build();

            // Test different log types with metadata
            LogContextManager.push("TestComponent");

            publisher.successBuilder("Operation completed successfully")
                .withMeta(Map.of("operation", "data_transform", "records", 100))
                .log();

            publisher.error("Processing failed")
                .withErrorCode("PROC_ERR_001")
                .withMeta(Map.of("failedRecords", 5, "retryable", true))
                .log();

            publisher.warnBuilder("Memory usage high")
                .withMeta(Map.of("memoryUsage", "85%", "threshold", "80%"))
                .log();

        } finally {
            LogContextManager.pop();
            LogContextManager.cleanup(runId.toString());
        }

        // Allow time for processing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify metadata is properly captured
        assertEquals(3, capturedLogs.size(), "Should have 3 log entries");

        capturedLogs.forEach(log -> {
            // Verify basic correlation fields
            assertEquals(workflowId, log.getWorkflowId());
            assertEquals(runId, log.getWorkflowRunId());
            assertEquals("MetadataTestNode", log.getNodeKey());
            assertEquals(traceId, log.getTraceId());
            assertEquals("TestComponent", log.getHierarchy());
            assertEquals(userId, log.getUserId());
            assertNotNull(log.getTimestamp());
        });

        // Verify specific metadata
        LogEntry successLog = capturedLogs.stream()
            .filter(log -> log.getLevel() == LogLevel.SUCCESS)
            .findFirst().orElseThrow();
        assertNotNull(successLog.getMeta());
        assertEquals("data_transform", successLog.getMeta().get("operation"));
        assertEquals(100, successLog.getMeta().get("records"));

        LogEntry errorLog = capturedLogs.stream()
            .filter(log -> log.getLevel() == LogLevel.ERROR)
            .findFirst().orElseThrow();
        assertEquals("PROC_ERR_001", errorLog.getErrorCode());
        assertNotNull(errorLog.getMeta());
        assertEquals(5, errorLog.getMeta().get("failedRecords"));
        assertEquals(true, errorLog.getMeta().get("retryable"));
    }
}
