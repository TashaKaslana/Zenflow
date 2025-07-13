package org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.workflow_run.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.enums.WorkflowStatus;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {

    /**
     * Find workflow runs by workflow ID
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.workflow.id = :workflowId ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findByWorkflowId(@Param("workflowId") UUID workflowId);

    /**
     * Find workflow runs by workflow ID with pagination
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.workflow.id = :workflowId ORDER BY wr.startedAt DESC")
    Page<WorkflowRun> findByWorkflowId(@Param("workflowId") UUID workflowId, Pageable pageable);

    /**
     * Find workflow runs by status
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.status = :status ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findByStatus(@Param("status") WorkflowStatus status);

    /**
     * Find workflow runs by workflow ID and status
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.workflow.id = :workflowId AND wr.status = :status ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findByWorkflowIdAndStatus(@Param("workflowId") UUID workflowId, @Param("status") WorkflowStatus status);

    /**
     * Find workflow runs by trigger type
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.triggerType = :triggerType ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findByTriggerType(@Param("triggerType") TriggerType triggerType);

    /**
     * Find running workflow runs (status = RUNNING or WAITING)
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.status IN ('RUNNING', 'WAITING') ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findRunningWorkflowRuns();

    /**
     * Find completed workflow runs (status = SUCCESS or ERROR)
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.status IN ('SUCCESS', 'ERROR') ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findCompletedWorkflowRuns();

    /**
     * Find latest workflow run for a workflow
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.workflow.id = :workflowId ORDER BY wr.startedAt DESC LIMIT 1")
    Optional<WorkflowRun> findLatestByWorkflowId(@Param("workflowId") UUID workflowId);

    /**
     * Find workflow runs within date range
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.startedAt BETWEEN :startDate AND :endDate ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findByDateRange(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    /**
     * Find workflow runs by workflow ID within date range
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.workflow.id = :workflowId AND wr.startedAt BETWEEN :startDate AND :endDate ORDER BY wr.startedAt DESC")
    List<WorkflowRun> findByWorkflowIdAndDateRange(@Param("workflowId") UUID workflowId,
                                                   @Param("startDate") OffsetDateTime startDate,
                                                   @Param("endDate") OffsetDateTime endDate);

    /**
     * Count workflow runs by workflow ID
     */
    @Query("SELECT COUNT(wr) FROM WorkflowRun wr WHERE wr.workflow.id = :workflowId")
    long countByWorkflowId(@Param("workflowId") UUID workflowId);

    /**
     * Count workflow runs by workflow ID and status
     */
    @Query("SELECT COUNT(wr) FROM WorkflowRun wr WHERE wr.workflow.id = :workflowId AND wr.status = :status")
    long countByWorkflowIdAndStatus(@Param("workflowId") UUID workflowId, @Param("status") WorkflowStatus status);

    /**
     * Find long-running workflow runs (running for more than specified duration)
     */
    @Query("SELECT wr FROM WorkflowRun wr WHERE wr.status IN ('RUNNING', 'WAITING') AND wr.startedAt < :cutoffTime ORDER BY wr.startedAt ASC")
    List<WorkflowRun> findLongRunningWorkflowRuns(@Param("cutoffTime") OffsetDateTime cutoffTime);
}
