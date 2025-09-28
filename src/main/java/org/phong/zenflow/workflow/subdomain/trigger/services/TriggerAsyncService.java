package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TriggerAsyncService {
  private final WorkflowTriggerRepository repo;

  @Async("applicationTaskExecutor")
  @Transactional // opens a transaction on this async thread
  public void markTriggeredAsync(UUID triggerId, Instant at) {
    repo.updateLastTriggeredAt(triggerId, at);
  }
}