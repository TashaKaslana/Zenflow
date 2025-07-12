package org.phong.zenflow.project.infrastructure.persistence.repository;

import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
}