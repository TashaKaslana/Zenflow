package org.phong.zenflow.secret.infrastructure.persistence.repository;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SecretRepository extends JpaRepository<Secret, UUID> {

    @Query("SELECT s FROM Secret s WHERE s.user.id = :userId AND s.deletedAt IS NULL")
    List<Secret> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM Secret s WHERE s.project.id = :projectId AND s.deletedAt IS NULL")
    List<Secret> findByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT s FROM Secret s WHERE s.workflow.id = :workflowId AND s.deletedAt IS NULL")
    List<Secret> findByWorkflowId(@Param("workflowId") UUID workflowId);

    boolean existsByGroupNameAndWorkflow_Id(@NotNull String groupName, @NotNull UUID workflow_id);
}