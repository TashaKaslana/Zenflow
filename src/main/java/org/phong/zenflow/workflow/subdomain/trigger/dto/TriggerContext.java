package org.phong.zenflow.workflow.subdomain.trigger.dto;

import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;

import java.util.Map;

public record TriggerContext(WorkflowTrigger trigger, Map<String, String> profiles) {

}
