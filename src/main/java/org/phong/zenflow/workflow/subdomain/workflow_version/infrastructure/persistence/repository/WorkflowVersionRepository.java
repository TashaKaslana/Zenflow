package org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.persistence.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {
    Optional<WorkflowVersion> findTopByWorkflow_IdOrderByVersionDesc(UUID workflowId);

    List<WorkflowVersion> findByWorkflow_IdOrderByVersionDesc(UUID workflowId);

    Optional<WorkflowVersion> findByWorkflow_IdAndVersion(UUID workflowId, Integer version);

    @Modifying
    void deleteByWorkflow_IdAndCreatedAtBefore(UUID workflowId, OffsetDateTime createdAt);

    @Modifying
    void deleteByWorkflow_IdAndVersion(UUID workflowId, Integer version);

    @Modifying
    void deleteByWorkflow_Id(UUID workflowId);
}

