package org.phong.zenflow.workflow.subdomain.trigger.impl;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.runner.service.WorkflowRunnerService;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.UUID;

@AllArgsConstructor
public class ScheduledWorkflowJob extends QuartzJobBean {
    private final WorkflowRunnerService runnerService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        String workflowIdStr = context.getJobDetail().getJobDataMap().getString("workflowId");
        UUID workflowId = UUID.fromString(workflowIdStr);

        runnerService.runWorkflow(workflowId, TriggerType.SCHEDULE, workflowId, null);
    }
}
