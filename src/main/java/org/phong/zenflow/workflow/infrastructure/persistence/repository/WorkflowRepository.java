package org.phong.zenflow.workflow.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    /**
     * Find workflows by project ID
     */
    @Query("SELECT w FROM Workflow w WHERE w.project.id = :projectId AND w.deletedAt IS NULL")
    List<Workflow> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find workflows by project ID and active status
     */
    @Query("SELECT w FROM Workflow w WHERE w.project.id = :projectId AND w.isActive = :isActive AND w.deletedAt IS NULL")
    List<Workflow> findByProjectIdAndIsActive(@Param("projectId") UUID projectId, @Param("isActive") Boolean isActive);

    /**
     * Count workflows by project ID
     */
    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.project.id = :projectId AND w.deletedAt IS NULL")
    long countByProjectId(@Param("projectId") UUID projectId);

    /**
     * Count workflows by project ID and active status
     */
    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.project.id = :projectId AND w.isActive = :isActive AND w.deletedAt IS NULL")
    long countByProjectIdAndIsActive(@Param("projectId") UUID projectId, @Param("isActive") Boolean isActive);
}