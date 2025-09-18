package org.phong.zenflow.secret.subdomain.link.infrastructure.repository;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretProfileNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretProfileNodeLinkInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecretProfileNodeLinkRepository extends JpaRepository<SecretProfileNodeLink, UUID> {
    Optional<SecretProfileNodeLink> findByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    void deleteByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    void deleteAllByWorkflowId(UUID workflowId);
    java.util.List<SecretProfileNodeLink> findAllByWorkflowId(UUID workflowId);

    @Query("SELECT l.id, l.profile.id, l.nodeKey FROM SecretProfileNodeLink l WHERE l.workflow.id = :workflowId")
    List<SecretProfileNodeLinkInfo> getProfileLinksByWorkflowId(UUID workflowId);
}
