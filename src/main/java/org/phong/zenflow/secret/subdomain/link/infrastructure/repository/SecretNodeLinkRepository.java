package org.phong.zenflow.secret.subdomain.link.infrastructure.repository;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretNodeLinkInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SecretNodeLinkRepository extends JpaRepository<SecretNodeLink, UUID> {
    Optional<SecretNodeLink> findByWorkflowIdAndNodeKeyAndSecretId(UUID workflowId, String nodeKey, UUID secretId);
    List<SecretNodeLink> findByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    void deleteByWorkflowIdAndNodeKeyAndSecretId(UUID workflowId, String nodeKey, UUID secretId);
    void deleteByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
    List<SecretNodeLink> findByWorkflowId(UUID workflowId);

    @Modifying
    @Query(
            "DELETE FROM SecretNodeLink nk WHERE nk.workflow.id = :workflowId AND nk.secret.id IN :secretIds"
    )
    void deleteAllByIdAndSecretIdList(@Param("workflowId") UUID workflowId, @Param("secretIds") Set<UUID> secretIds);

    void deleteAllByWorkflowId(UUID workflowId);

    @Query("SELECT id, secret.id, nodeKey FROM SecretNodeLink WHERE workflow.id = :workflowId")
    List<SecretNodeLinkInfo> getSecretNodeLinkInfoByWorkflowId(UUID workflowId);
}
