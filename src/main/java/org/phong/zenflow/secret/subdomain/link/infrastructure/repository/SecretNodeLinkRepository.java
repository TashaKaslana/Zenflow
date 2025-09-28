package org.phong.zenflow.secret.subdomain.link.infrastructure.repository;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretNodeLinkInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecretNodeLinkRepository extends JpaRepository<SecretNodeLink, UUID> {
    Optional<SecretNodeLink> findByWorkflowIdAndNodeKeyAndSecretId(UUID workflowId, String nodeKey, UUID secretId);
    List<SecretNodeLink> findByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    void deleteByWorkflowIdAndNodeKeyAndSecretId(UUID workflowId, String nodeKey, UUID secretId);
    void deleteByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    List<SecretNodeLink> findByWorkflowId(UUID workflowId);

    void deleteAllByWorkflowId(UUID workflowId);

    @Query("""
       select l.id as id,
              s.id as secretId,
              l.nodeKey as nodeKey
       from SecretNodeLink l
       join l.workflow w
       join l.secret s
       where w.id = :workflowId
       """)
    List<SecretNodeLinkInfo> getSecretNodeLinkInfoByWorkflowId(UUID workflowId);
}
