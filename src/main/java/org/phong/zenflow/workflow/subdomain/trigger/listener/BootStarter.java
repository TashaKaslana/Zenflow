package org.phong.zenflow.workflow.subdomain.trigger.listener;

import org.phong.zenflow.workflow.subdomain.trigger.services.TriggerOrchestrator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BootStarter implements CommandLineRunner {
  private final TriggerOrchestrator orchestrator;
  public BootStarter(TriggerOrchestrator orchestrator) { this.orchestrator = orchestrator; }
  @Override public void run(String... args) { orchestrator.startAllEnabled(); }
}
