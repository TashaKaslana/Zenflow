package org.phong.zenflow.plugin.subdomain.executors.builtin.flow.timeout;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowEngineService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class TimeoutResumeJob implements Job {
    private WorkflowEngineService workflowEngine;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        UUID workflowId = UUID.fromString(dataMap.getString("workflowId"));
        UUID workflowRunId = UUID.fromString(dataMap.getString("workflowRunId"));
        String nodeKey = dataMap.getString("nodeKey");

        log.info("Resuming timeout node: {} in run {}", nodeKey, workflowRunId);
        workflowEngine.runWorkflow(workflowId, workflowRunId, nodeKey);
    }
}
