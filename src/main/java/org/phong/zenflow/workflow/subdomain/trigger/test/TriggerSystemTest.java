package org.phong.zenflow.workflow.subdomain.trigger.test;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.services.TriggerOrchestrator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test component to demonstrate the trigger system with resource management.
 * This will run automatically when the application starts.
 */
@Component
@Slf4j
@AllArgsConstructor
public class TriggerSystemTest {

    private TriggerOrchestrator orchestrator;

    @EventListener(ApplicationReadyEvent.class)
    public void testTriggerSystem() {
        log.info("=== TESTING TRIGGER SYSTEM WITH RESOURCE MANAGEMENT ===");

        try {
            // Test 1: Create a simple schedule trigger
            testScheduleTrigger();

            // Test 2: Show how resource sharing would work (simulated)
            testResourceSharing();

            // Test 3: Test status monitoring
            testStatusMonitoring();

        } catch (Exception e) {
            log.error("Error in trigger system test: {}", e.getMessage(), e);
        }
    }

    private void testScheduleTrigger() {
        log.info("--- Test 1: Schedule Trigger ---");

        // Test 1A: Interval-based trigger
        log.info("Testing interval-based scheduling...");
        testIntervalTrigger();

        // Test 1B: Cron-based trigger
        log.info("Testing cron-based scheduling...");
        testCronTrigger();
    }

    private void testIntervalTrigger() {
        // Create interval-based trigger
        WorkflowTrigger trigger = new WorkflowTrigger();
        trigger.setId(UUID.randomUUID());
        trigger.setWorkflowId(UUID.randomUUID());
        trigger.setType(TriggerType.SCHEDULE);
        trigger.setEnabled(true);

        // Set the trigger executor ID
        UUID scheduleExecutorId = UUID.fromString("df3198f4-bc08-48cf-8bae-b0d5b83aee45");
        trigger.setTriggerExecutorId(scheduleExecutorId);

        // Configure interval-based scheduling
        Map<String, Object> config = new HashMap<>();
        config.put("interval_seconds", 3);
        config.put("description", "Test interval trigger - every 3 seconds");
        trigger.setConfig(config);

        log.info("Created interval trigger: {} for workflow: {} (3-second interval)",
                trigger.getId(), trigger.getWorkflowId());

        // Start the trigger
        orchestrator.start(trigger);

        try {
            Thread.sleep(8000); // Let it fire 2-3 times
            orchestrator.stop(trigger.getId());
            log.info("Interval trigger test completed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void testCronTrigger() {
        // Create cron-based trigger
        WorkflowTrigger cronTrigger = new WorkflowTrigger();
        cronTrigger.setId(UUID.randomUUID());
        cronTrigger.setWorkflowId(UUID.randomUUID());
        cronTrigger.setType(TriggerType.SCHEDULE);
        cronTrigger.setEnabled(true);

        // Set the trigger executor ID
        UUID scheduleExecutorId = UUID.fromString("df3198f4-bc08-48cf-8bae-b0d5b83aee45");
        cronTrigger.setTriggerExecutorId(scheduleExecutorId);

        // Configure cron-based scheduling (every 5 seconds)
        Map<String, Object> cronConfig = new HashMap<>();
        cronConfig.put("cron_expression", "*/5 * * * * ?"); // Every 5 seconds
        cronConfig.put("description", "Test cron trigger - every 5 seconds");
        cronTrigger.setConfig(cronConfig);

        log.info("Created cron trigger: {} for workflow: {} (cron: '*/5 * * * * ?')",
                cronTrigger.getId(), cronTrigger.getWorkflowId());

        // Start the trigger
        orchestrator.start(cronTrigger);

        try {
            Thread.sleep(12000); // Let it fire 2-3 times
            orchestrator.stop(cronTrigger.getId());
            log.info("Cron trigger test completed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void testResourceSharing() {
        log.info("--- Test 2: Resource Sharing Simulation ---");

        // This demonstrates how multiple triggers would share resources
        // (In real Discord example, multiple triggers with same bot token = 1 JDA instance)

        log.info("Resource sharing example:");
        log.info("- Trigger A uses Discord bot token 'ABC123' -> Creates JDA instance #1");
        log.info("- Trigger B uses Discord bot token 'ABC123' -> Reuses JDA instance #1");
        log.info("- Trigger C uses Discord bot token 'XYZ789' -> Creates JDA instance #2");
        log.info("- When Trigger A stops -> JDA instance #1 stays (Trigger B still using it)");
        log.info("- When Trigger B stops -> JDA instance #1 cleaned up (no more users)");
        log.info("- When Trigger C stops -> JDA instance #2 cleaned up");

        log.info("This prevents the '1000 triggers = 1000 connections' problem!");
    }

    private void testStatusMonitoring() {
        log.info("--- Test 3: Status Monitoring ---");

        Map<UUID, String> runningStatus = orchestrator.getRunningStatus();
        log.info("Currently running triggers: {}", runningStatus.size());

        runningStatus.forEach((id, status) -> {
            log.info("Trigger {}: {}", id, status);
        });
    }
}
