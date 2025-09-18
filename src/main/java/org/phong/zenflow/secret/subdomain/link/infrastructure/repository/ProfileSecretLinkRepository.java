package org.phong.zenflow.secret.subdomain.link.infrastructure.repository;

import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.ProfileSecretLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProfileSecretLinkRepository extends JpaRepository<ProfileSecretLink, UUID> {

    @Query("SELECT l FROM ProfileSecretLink l WHERE l.profile.workflow.id = :workflowId")
    List<ProfileSecretLink> findByWorkflowId(@Param("workflowId") UUID workflowId);
}

