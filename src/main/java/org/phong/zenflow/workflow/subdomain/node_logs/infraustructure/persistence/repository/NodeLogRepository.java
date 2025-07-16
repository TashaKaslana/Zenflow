package org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.repository;

import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NodeLogRepository extends JpaRepository<NodeLog, UUID> {
  List<NodeLog> findByWorkflowRunId(UUID workflowRunId);

  Optional<NodeLog> findByWorkflowRunIdAndNodeKey(UUID workflowRunId, String nodeKey);
}
