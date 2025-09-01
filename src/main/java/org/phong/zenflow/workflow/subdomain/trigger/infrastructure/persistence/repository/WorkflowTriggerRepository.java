package org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowTriggerRepository extends JpaRepository<WorkflowTrigger, UUID> {
    List<WorkflowTrigger> findByWorkflowId(UUID workflowId);

    @Query(value = "SELECT * FROM workflow_triggers t " +
            "WHERE t.config->>'custom_path' = :identifier " +
            "AND t.enabled = true", nativeQuery = true)
    Optional<WorkflowTrigger> findByCustomPath(@Param("identifier") String identifier);

    Iterable<WorkflowTrigger> findByEnabledTrue();

    Optional<WorkflowTrigger> findByWorkflowIdAndTriggerExecutorId(UUID workflowId, UUID nodeId);

    @Modifying
    @Query(
            "UPDATE WorkflowTrigger t SET t.lastTriggeredAt = :now WHERE t.id = :triggerId"
    )
    void updateLastTriggeredAt(UUID triggerId, Instant now);
}