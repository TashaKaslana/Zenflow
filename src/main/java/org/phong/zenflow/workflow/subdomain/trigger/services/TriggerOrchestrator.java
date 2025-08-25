package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
public class TriggerOrchestrator {

  private final WorkflowTriggerRepository repo;
  private final Map<UUID, TriggerExecutor.RunningHandle> running = new ConcurrentHashMap<>();
  private final TriggerContext ctx;
  private final TriggerRegistry registry;

  public void startAllEnabled() {
    repo.findByEnabledTrue().forEach(this::start);
  }

  public synchronized void start(WorkflowTrigger t) {
//    if (running.containsKey(t.getId())) return;
////    TriggerExecutor ex = registry.getRegistry(PluginNodeIdentifier.fromString(""));
//    if (ex == null) return; // silently ignore unknown types
//    try {
//      var handle = ex.start(t, ctx);
//      running.put(t.getId(), handle);
//    } catch (Exception e) {
//      log.error(e.getMessage(), e);
//    }
  }

  public synchronized void stop(UUID triggerId) {
    var handle = running.remove(triggerId);
    if (handle != null) handle.stop();
  }

  public synchronized void restart(WorkflowTrigger t) {
    stop(t.getId());
    if (t.getEnabled()) start(t);
  }

  // Safety reconciler: ensure DB truth matches runtime
  public void reconcile() {
    var desired = new HashSet<UUID>();
    for (WorkflowTrigger t : repo.findByEnabledTrue()) {
      desired.add(t.getId());
      if (!running.containsKey(t.getId())) start(t);
    }
    // stop any extra
    for (UUID runningId : new ArrayList<>(running.keySet())) {
      if (!desired.contains(runningId)) stop(runningId);
    }
  }
}
