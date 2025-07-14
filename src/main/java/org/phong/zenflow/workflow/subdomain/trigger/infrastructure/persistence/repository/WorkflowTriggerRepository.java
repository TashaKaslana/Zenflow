package org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowTriggerRepository extends JpaRepository<WorkflowTrigger, UUID> {
    List<WorkflowTrigger> findByWorkflowId(UUID workflowId);
}