package org.phong.zenflow.workflow.subdomain.trigger.listener;

import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.services.WorkflowSchedulerService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppStartup implements ApplicationRunner {

    private final WorkflowTriggerRepository repository;
    private final WorkflowSchedulerService schedulerService;

    public AppStartup(WorkflowTriggerRepository repository, WorkflowSchedulerService schedulerService) {
        this.repository = repository;
        this.schedulerService = schedulerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<WorkflowTrigger> triggers = repository.findAllByTypeAndEnabled(TriggerType.SCHEDULE, true);
        schedulerService.registerAll(triggers);
    }
}
