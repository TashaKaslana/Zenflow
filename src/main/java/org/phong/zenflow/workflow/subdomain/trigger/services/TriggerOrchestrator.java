package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TriggerOrchestrator {

  private final WorkflowTriggerRepository repo;
  private final Map<UUID, TriggerExecutor.RunningHandle> running = new ConcurrentHashMap<>();
  private final Map<TriggerType, TriggerExecutor> executors = new EnumMap<>(TriggerType.class);
  private final TriggerContext ctx;

  public TriggerOrchestrator(
      WorkflowTriggerRepository repo,
      List<TriggerExecutor> executorBeans,
      TriggerContext ctx
  ) {
    this.repo = repo;
    this.ctx = ctx;
    // Wire executors discovered from Spring
    executorBeans.forEach(ex -> {
      // map by supported type; for simplicity we cast where needed
//      if (ex.getClass().getSimpleName().toLowerCase().contains("discord")) {
//        executors.put(TriggerType.discord, ex);
//      }
      // add more mappings for webhook/schedule, etc.
    });
  }

  public void startAllEnabled() {
    repo.findByEnabledTrue().forEach(this::start);
  }

  public synchronized void start(WorkflowTrigger t) {
    if (running.containsKey(t.getId())) return;
    TriggerExecutor ex = executors.get(t.getType());
    if (ex == null) return; // silently ignore unknown types
    try {
      var handle = ex.start(t, ctx);
      running.put(t.getId(), handle);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
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
