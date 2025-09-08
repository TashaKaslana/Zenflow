package org.phong.zenflow.secret.infrastructure.persistence.repository;

import org.phong.zenflow.secret.infrastructure.persistence.entity.SecretProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SecretProfileRepository extends JpaRepository<SecretProfile, UUID> {
    @Query("SELECT COUNT(sp) > 0 FROM SecretProfile sp WHERE sp.name = :name AND sp.workflow.id = :workflowId AND sp.plugin.id = :pluginId")
    boolean existsByNameAndWorkflowIdAndPluginId(@Param("name") String name, @Param("workflowId") UUID workflowId, @Param("pluginId") UUID pluginId);
}
