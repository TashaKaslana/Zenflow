package org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.entity.NodeExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NodeExecutionRepository extends JpaRepository<NodeExecution, UUID> {
  List<NodeExecution> findByWorkflowRunId(UUID workflowRunId);

  Optional<NodeExecution> findTopByWorkflowRunIdAndNodeKeyOrderByStartedAtDesc(UUID workflowRunId, String nodeKey);
}
