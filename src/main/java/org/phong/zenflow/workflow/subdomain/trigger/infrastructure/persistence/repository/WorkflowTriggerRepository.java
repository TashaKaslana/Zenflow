package org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowTriggerRepository extends JpaRepository<WorkflowTrigger, UUID> {
    List<WorkflowTrigger> findByWorkflowId(UUID workflowId);

    List<WorkflowTrigger> findAllByTypeAndEnabled(TriggerType triggerType, boolean b);

    @Query(value = "SELECT * FROM workflow_triggers t " +
            "WHERE t.config->>'custom_path' = :identifier " +
            "AND t.enabled = true", nativeQuery = true)
    Optional<WorkflowTrigger> findByCustomPath(@Param("identifier") String identifier);

    Iterable<WorkflowTrigger> findByEnabledTrue();
}