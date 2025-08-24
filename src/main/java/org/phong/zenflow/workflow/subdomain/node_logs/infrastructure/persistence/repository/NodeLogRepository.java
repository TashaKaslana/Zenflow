package org.phong.zenflow.workflow.subdomain.node_logs.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.node_logs.infrastructure.persistence.entity.NodeLog;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NodeLogRepository extends JpaRepository<NodeLog, UUID> {

    Page<NodeLog> findByWorkflowRunId(UUID workflowRunId, Pageable pageable);

    Page<NodeLog> findByWorkflowRunIdAndNodeKey(UUID workflowRunId, String nodeKey, Pageable pageable);

    Page<NodeLog> findByWorkflowRunIdAndLevel(UUID workflowRunId, LogLevel level, Pageable pageable);

    @Query("SELECT nl FROM NodeLog nl WHERE nl.workflowRun.id = :workflowRunId " +
           "AND nl.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY nl.timestamp DESC")
    List<NodeLog> findByWorkflowRunIdAndTimestampBetween(@Param("workflowRunId") UUID workflowRunId,
                                                         @Param("startTime") OffsetDateTime startTime,
                                                         @Param("endTime") OffsetDateTime endTime);

    @Query("SELECT nl FROM NodeLog nl WHERE nl.workflowRun.id = :workflowRunId " +
           "AND nl.nodeKey = :nodeKey AND nl.level IN :levels " +
           "ORDER BY nl.timestamp DESC")
    List<NodeLog> findByWorkflowRunIdAndNodeKeyAndLevelIn(@Param("workflowRunId") UUID workflowRunId,
                                                          @Param("nodeKey") String nodeKey,
                                                          @Param("levels") List<LogLevel> levels);

    @Query("SELECT nl FROM NodeLog nl WHERE nl.correlationId = :correlationId ORDER BY nl.timestamp DESC")
    List<NodeLog> findByCorrelationId(@Param("correlationId") String correlationId);

    long countByWorkflowRunIdAndLevel(UUID workflowRunId, LogLevel level);

    void deleteByWorkflowRunId(UUID workflowRunId);
}
