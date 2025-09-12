package org.phong.zenflow.secret.infrastructure.persistence.repository;

import org.phong.zenflow.secret.infrastructure.persistence.entity.SecretNodeLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecretNodeLinkRepository extends JpaRepository<SecretNodeLink, UUID> {
    Optional<SecretNodeLink> findByWorkflowIdAndNodeKeyAndSecretId(UUID workflowId, String nodeKey, UUID secretId);
    List<SecretNodeLink> findByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    void deleteByWorkflowIdAndNodeKeyAndSecretId(UUID workflowId, String nodeKey, UUID secretId);
    void deleteByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    List<SecretNodeLink> findByWorkflowId(UUID workflowId);
}
