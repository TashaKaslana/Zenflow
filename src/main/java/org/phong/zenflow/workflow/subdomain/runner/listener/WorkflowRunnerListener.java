package org.phong.zenflow.workflow.subdomain.runner.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.runner.service.WorkflowRunnerService;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class WorkflowRunnerListener {
    private WorkflowRunnerService workflowRunnerService;


}
