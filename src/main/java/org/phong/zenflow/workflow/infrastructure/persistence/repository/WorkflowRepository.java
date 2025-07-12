package org.phong.zenflow.workflow.infrastructure.persistence.repository;

import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
}