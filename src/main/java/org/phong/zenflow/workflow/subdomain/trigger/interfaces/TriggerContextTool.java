package org.phong.zenflow.workflow.subdomain.trigger.interfaces;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface TriggerContextTool {
  // Minimal contract the executor needs to start a workflow
  void startWorkflow(UUID workflowId, UUID triggerExecutorId, Map<String, Object> payload);

  // Optional: update last triggered time
  void markTriggered(UUID triggerId, Instant at);
}
