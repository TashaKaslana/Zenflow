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
    List<SecretProfileNodeLink> findAllByWorkflowId(UUID workflowId);

    @Query("""
       select l.id as id,
              p.id as profileId,
              l.nodeKey as nodeKey
       from SecretProfileNodeLink l
       join l.profile p
       where l.workflow.id = :workflowId
       """)
    List<SecretProfileNodeLinkInfo> getProfileLinksByWorkflowId(UUID workflowId);

    @Query("SELECT id FROm SecretProfileNodeLink WHERE workflow.id = :workflowId")
    List<UUID> findAllIdsByWorkflowId(UUID workflowId);
}
