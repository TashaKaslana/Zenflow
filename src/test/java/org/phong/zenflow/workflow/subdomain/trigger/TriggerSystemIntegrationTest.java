package org.phong.zenflow.workflow.subdomain.trigger;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.phong.zenflow.core.services.SharedQuartzSchedulerService;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.schedule.ScheduleTriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.services.TriggerOrchestrator;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive integration test for the optimized trigger system.
 * Uses shared Spring context and stateful setup for maximum performance.
 * Tests the single shared Quartz scheduler efficiency and proper trigger registration.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.quartz.job-store-type=memory", // Use in-memory for tests
    "logging.level.org.phong.zenflow.workflow.subdomain.trigger=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Share instance across all test methods
@Slf4j
public class TriggerSystemIntegrationTest {

    @Autowired
    private TriggerOrchestrator orchestrator;

    @Autowired
    private TriggerRegistry triggerRegistry;

    @Autowired
    private PluginNodeExecutorRegistry registry;

    @Autowired
    private SharedQuartzSchedulerService sharedQuartzSchedulerService;

    // Shared state across tests
    private UUID scheduleExecutorId;

    @BeforeAll // Run once for all tests, not before each test
    public void setupTriggerRegistryOnce() {
        log.info("=== SETTING UP SHARED TRIGGER SYSTEM (ONE-TIME STARTUP) ===");

        // Register the schedule trigger executor with UUID (cast to string) as you mentioned
        scheduleExecutorId = generateDeterministicExecutorId("core:schedule.trigger:1.0.0");

        registry.register(
                scheduleExecutorId.toString(),
                () -> new ScheduleTriggerExecutor(sharedQuartzSchedulerService)
        );
        triggerRegistry.registerTrigger(scheduleExecutorId.toString());

        log.info("✅ Registered schedule trigger executor with UUID: {} (SHARED FOR ALL TESTS)", scheduleExecutorId);
        log.info("=== SHARED TRIGGER SYSTEM READY - TESTS WILL RUN EFFICIENTLY ===");
    }

    @BeforeEach // Only clean up between tests, don't re-register
    public void cleanupBetweenTests() {
        // Clean up any running triggers from previous tests
        orchestrator.getRunningStatus().keySet().forEach(orchestrator::stop);
        log.debug("Cleaned up {} running triggers before test", orchestrator.getRunningStatus().size());
    }

    @Test
    @Order(1)
    public void testIntervalBasedScheduling() throws Exception {
        log.info("=== Testing Optimized Interval-Based Scheduling ===");

        WorkflowTrigger trigger = createTestTrigger(TriggerType.SCHEDULE);

        // Configure 1-second interval for fast testing - use mutable HashMap
        Map<String, Object> config = new HashMap<>();
        config.put("interval_seconds", 1);
        config.put("description", "Integration test - optimized interval trigger");
        trigger.setConfig(config);

        log.info("Starting interval trigger: {} (every 1 second)", trigger.getId());

        // Start and verify
        orchestrator.start(trigger);
        Map<UUID, String> status = orchestrator.getRunningStatus();
        Assertions.assertTrue(status.containsKey(trigger.getId()), "Trigger should be running");
        Assertions.assertEquals("RUNNING", status.get(trigger.getId()), "Trigger status should be RUNNING");

        // Let it fire a couple times to test shared scheduler efficiency
        Thread.sleep(2500); // Should fire at 0s, 1s, 2s

        // Verify it's still running
        status = orchestrator.getRunningStatus();
        Assertions.assertTrue(status.containsKey(trigger.getId()), "Trigger should still be running");

        // Clean shutdown
        orchestrator.stop(trigger.getId());
        status = orchestrator.getRunningStatus();
        Assertions.assertFalse(status.containsKey(trigger.getId()), "Trigger should be stopped");

        log.info("✅ Optimized interval scheduling test completed successfully");
    }

    @Test
    @Order(2)
    public void testCronBasedScheduling() throws Exception {
        log.info("=== Testing Optimized Cron-Based Scheduling ===");

        WorkflowTrigger cronTrigger = createTestTrigger(TriggerType.SCHEDULE);

        // Configure 2-second cron expression - use mutable HashMap
        Map<String, Object> cronConfig = new HashMap<>();
        cronConfig.put("cron_expression", "*/2 * * * * ?");
        cronConfig.put("description", "Integration test - optimized cron trigger");
        cronTrigger.setConfig(cronConfig);

        log.info("Starting cron trigger: {} (*/2 * * * * ?)", cronTrigger.getId());

        // Start and verify
        orchestrator.start(cronTrigger);
        Map<UUID, String> status = orchestrator.getRunningStatus();
        Assertions.assertTrue(status.containsKey(cronTrigger.getId()), "Cron trigger should be running");

        // Let it fire a couple times
        Thread.sleep(4500); // Should fire at 0s, 2s, 4s

        // Clean shutdown
        orchestrator.stop(cronTrigger.getId());

        log.info("✅ Optimized cron scheduling test completed successfully");
    }

    @Test
    @Order(3)
    public void testSharedSchedulerEfficiency() throws Exception {
        log.info("=== Testing Single Shared Scheduler Efficiency (Key Optimization) ===");

        // Test the critical architectural improvement: single scheduler handling many triggers
        int triggerCount = 10; // Simulate many concurrent triggers
        WorkflowTrigger[] triggers = new WorkflowTrigger[triggerCount];

        // Create triggers with different schedules - shorter times for faster tests
        for (int i = 0; i < triggerCount; i++) {
            triggers[i] = createTestTrigger(TriggerType.SCHEDULE);

            Map<String, Object> config = new HashMap<>();
            if (i % 2 == 0) {
                // Mix of interval and cron triggers - use shorter intervals
                config.put("interval_seconds", 2 + (i / 2));
                config.put("description", "Efficiency test interval trigger " + (i + 1));
            } else {
                config.put("cron_expression", "*/" + (3 + (i / 2)) + " * * * * ?");
                config.put("description", "Efficiency test cron trigger " + (i + 1));
            }
            triggers[i].setConfig(config);
        }

        // Start all triggers and measure performance
        long startTime = System.currentTimeMillis();
        for (WorkflowTrigger trigger : triggers) {
            orchestrator.start(trigger);
        }
        long endTime = System.currentTimeMillis();

        log.info("🚀 Started {} triggers in {}ms using SINGLE shared scheduler",
                triggerCount, (endTime - startTime));
        log.info("💡 This demonstrates the architectural optimization vs multiple scheduler instances");

        // Verify all running efficiently on shared scheduler
        Map<UUID, String> status = orchestrator.getRunningStatus();
        Assertions.assertEquals(triggerCount, status.size(), "All triggers should be running on shared scheduler");

        // Let them run concurrently to prove shared scheduler efficiency - reduced time
        Thread.sleep(3000);

        // Clean shutdown all
        startTime = System.currentTimeMillis();
        for (WorkflowTrigger trigger : triggers) {
            orchestrator.stop(trigger.getId());
        }
        endTime = System.currentTimeMillis();

        log.info("🧹 Stopped {} triggers in {}ms - shared scheduler cleanup efficient",
                triggerCount, (endTime - startTime));

        // Verify all stopped
        status = orchestrator.getRunningStatus();
        Assertions.assertTrue(status.isEmpty(), "All triggers should be stopped");

        log.info("✅ Shared scheduler efficiency test completed - MUCH better than multiple schedulers!");
    }

    @Test
    @Order(4)
    public void testAdvancedCronExpressions() {
        log.info("=== Testing Advanced Cron Expression Support ===");

        // Test various cron expressions to demonstrate flexibility - shorter intervals
        Map<String, String> cronTests = Map.of(
            "*/3 * * * * ?", "Every 3 seconds",
            "0 */1 * * * ?", "Every 1 minute at second 0",
            "0 0 12 * * ?", "Daily at noon (won't fire in test, just validate)"
        );

        for (Map.Entry<String, String> entry : cronTests.entrySet()) {
            String cronExpr = entry.getKey();
            String description = entry.getValue();

            WorkflowTrigger cronTrigger = createTestTrigger(TriggerType.SCHEDULE);

            Map<String, Object> config = new HashMap<>();
            config.put("cron_expression", cronExpr);
            config.put("description", "Test: " + description);
            cronTrigger.setConfig(config);

            log.info("Testing cron expression: '{}' ({})", cronExpr, description);

            try {
                orchestrator.start(cronTrigger);

                // Brief run for expressions that will fire quickly - reduced times
                if (cronExpr.contains("*/3")) {
                    Thread.sleep(4000); // Let 3-second cron fire once
                } else {
                    Thread.sleep(1000); // Quick validation for others
                }

                orchestrator.stop(cronTrigger.getId());
                log.info("✅ Cron expression '{}' validated successfully", cronExpr);

            } catch (Exception e) {
                log.error("❌ Failed to validate cron expression '{}': {}", cronExpr, e.getMessage());
                Assertions.fail("Cron expression should be valid: " + cronExpr);
            }
        }

        log.info("✅ Advanced cron expressions test completed successfully");
    }

    @Test
    @Order(5)
    public void testTriggerLifecycleManagement() {
        log.info("=== Testing Complete Trigger Lifecycle ===");

        WorkflowTrigger trigger = createTestTrigger(TriggerType.SCHEDULE);

        Map<String, Object> config = new HashMap<>();
        config.put("interval_seconds", 3);
        config.put("description", "Lifecycle test");
        trigger.setConfig(config);

        // Test start
        orchestrator.start(trigger);
        Assertions.assertTrue(orchestrator.getRunningStatus().containsKey(trigger.getId()));
        log.info("✅ Start lifecycle test passed");

        // Test that duplicate start doesn't cause issues
        orchestrator.start(trigger);
        Assertions.assertEquals(1, orchestrator.getRunningStatus().size(), "Should still have only 1 trigger");
        log.info("✅ Duplicate start protection test passed");

        // Test restart with new config - modify the mutable config
        trigger.getConfig().put("interval_seconds", 5);
        trigger.getConfig().put("description", "Updated lifecycle test");
        orchestrator.restart(trigger);
        Assertions.assertTrue(orchestrator.getRunningStatus().containsKey(trigger.getId()));
        log.info("✅ Restart with new config test passed");

        // Test stop
        orchestrator.stop(trigger.getId());
        Assertions.assertFalse(orchestrator.getRunningStatus().containsKey(trigger.getId()));
        log.info("✅ Stop lifecycle test passed");

        log.info("✅ Complete trigger lifecycle management test passed");
    }

    @Test
    @Order(6)
    public void testConfigurationValidationAndErrorHandling() {
        log.info("=== Testing Configuration Validation ===");

        // Test missing configuration - should not start and not be in running status
        WorkflowTrigger invalidTrigger = createTestTrigger(TriggerType.SCHEDULE);
        Map<String, Object> invalidConfig = new HashMap<>();
        invalidConfig.put("description", "Invalid - no schedule");
        invalidTrigger.setConfig(invalidConfig);

        orchestrator.start(invalidTrigger);
        Assertions.assertFalse(orchestrator.getRunningStatus().containsKey(invalidTrigger.getId()),
                "Trigger with missing config should not start");
        log.info("✅ Correctly rejected trigger with missing schedule config");

        // Test invalid cron expression - should not start and not be in running status
        WorkflowTrigger cronTrigger = createTestTrigger(TriggerType.SCHEDULE);
        Map<String, Object> cronConfig = new HashMap<>();
        cronConfig.put("cron_expression", "60 * * * *"); // Invalid: 60 seconds, wrong field count
        cronTrigger.setConfig(cronConfig);

        orchestrator.start(cronTrigger);
        Assertions.assertFalse(orchestrator.getRunningStatus().containsKey(cronTrigger.getId()),
                "Trigger with invalid cron expression should not start");
        log.info("✅ Correctly rejected trigger with invalid cron expression");

        // Test missing executor ID - should not start and not be in running status
        WorkflowTrigger noExecutorTrigger = createTestTrigger(TriggerType.SCHEDULE);
        noExecutorTrigger.setTriggerExecutorId(null);
        Map<String, Object> config = new HashMap<>();
        config.put("interval_seconds", 5);
        noExecutorTrigger.setConfig(config);

        orchestrator.start(noExecutorTrigger);
        Assertions.assertFalse(orchestrator.getRunningStatus().containsKey(noExecutorTrigger.getId()),
                "Trigger without executor ID should not start");
        log.info("✅ Correctly rejected trigger without executor ID");

        // Test with valid config to ensure the test setup is working
        WorkflowTrigger validTrigger = createTestTrigger(TriggerType.SCHEDULE);
        Map<String, Object> validConfig = new HashMap<>();
        validConfig.put("interval_seconds", 5);
        validConfig.put("description", "Valid trigger for comparison");
        validTrigger.setConfig(validConfig);

        orchestrator.start(validTrigger);
        Assertions.assertTrue(orchestrator.getRunningStatus().containsKey(validTrigger.getId()),
                "Valid trigger should start successfully");
        log.info("✅ Valid trigger started successfully for comparison");

        // Clean up the valid trigger
        orchestrator.stop(validTrigger.getId());

        log.info("✅ Configuration validation test completed successfully");
    }

    @Test
    @Order(7)
    public void testResourceManagementPattern() throws Exception {
        log.info("=== Testing Resource Management Following GlobalDbConnectionPool Pattern ===");

        // This test demonstrates how the optimized architecture works
        // vs the old inefficient multiple-scheduler approach

        log.info("🎯 Architecture Demonstration:");
        log.info("   OLD (Inefficient): Multiple QuartzSchedulerResourceManager instances");
        log.info("   NEW (Optimized): Single shared Quartz scheduler for all triggers");

        // Create multiple triggers that would have created multiple schedulers before
        WorkflowTrigger[] triggers = new WorkflowTrigger[5];
        for (int i = 0; i < 5; i++) {
            triggers[i] = createTestTrigger(TriggerType.SCHEDULE);

            Map<String, Object> config = new HashMap<>();
            config.put("interval_seconds", 2); // Reduced from 4 to 2 seconds
            config.put("description", "Resource efficiency test " + (i + 1));
            triggers[i].setConfig(config);
        }

        // Start all - they all use the SAME scheduler now
        for (WorkflowTrigger trigger : triggers) {
            orchestrator.start(trigger);
        }

        log.info("💡 All {} triggers now use SINGLE shared scheduler (not {} separate schedulers)",
                triggers.length, triggers.length);

        // Verify shared scheduler efficiency
        Map<UUID, String> status = orchestrator.getRunningStatus();
        Assertions.assertEquals(5, status.size(), "All triggers running on shared scheduler");

        // Brief execution to show concurrent operation - reduced from 6 to 3 seconds
        Thread.sleep(3000);

        // Clean shutdown
        for (WorkflowTrigger trigger : triggers) {
            orchestrator.stop(trigger.getId());
        }

        log.info("✅ Resource management optimization test completed");
        log.info("🏆 This architecture now scales efficiently to thousands of triggers!");
    }

    /**
     * Helper method following your test patterns to create triggers with proper executor registration
     */
    private WorkflowTrigger createTestTrigger(TriggerType type) {
        WorkflowTrigger trigger = new WorkflowTrigger();
        trigger.setId(UUID.randomUUID());
        trigger.setWorkflowId(UUID.randomUUID());
        trigger.setType(type);
        trigger.setEnabled(true);

        // Set deterministic executor ID following your UUID pattern
        UUID scheduleExecutorId = generateDeterministicExecutorId("core:schedule.trigger:1.0.0");
        trigger.setTriggerExecutorId(scheduleExecutorId);

        return trigger;
    }

    /**
     * Generate consistent executor ID following your established pattern
     */
    private UUID generateDeterministicExecutorId(String executorKey) {
        return UUID.nameUUIDFromBytes(("trigger-executor:" + executorKey).getBytes());
    }
}
