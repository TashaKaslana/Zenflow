package org.phong.zenflow.secret.subdomain.link.infrastructure.repository;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretProfileNodeLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SecretProfileNodeLinkRepository extends JpaRepository<SecretProfileNodeLink, UUID> {
    Optional<SecretProfileNodeLink> findByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    void deleteByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    java.util.List<SecretProfileNodeLink> findAllByWorkflowId(UUID workflowId);
}
