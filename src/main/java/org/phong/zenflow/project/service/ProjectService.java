package org.phong.zenflow.project.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.project.dto.CreateProjectRequest;
import org.phong.zenflow.project.dto.ProjectDto;
import org.phong.zenflow.project.dto.UpdateProjectRequest;
import org.phong.zenflow.project.exception.ProjectNotFoundException;
import org.phong.zenflow.project.infrastructure.mapstruct.ProjectMapper;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.phong.zenflow.project.infrastructure.persistence.repository.ProjectRepository;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.user.infrastructure.persistence.repositories.UserRepository;
import org.phong.zenflow.user.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    /**
     * Create a new project
     */
    @Transactional
    @AuditLog(action = AuditAction.PROJECT_CREATE, targetIdExpression = "returnObject.id")
    public ProjectDto createProject(CreateProjectRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId().toString()));

        Project project = projectMapper.toEntity(request);
        project.setUser(user);

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    /**
     * Create multiple projects in bulk
     */
    @Transactional
    @AuditLog(action = AuditAction.PROJECT_CREATE)
    public List<ProjectDto> createProjects(List<CreateProjectRequest> requests) {
        return requests.stream()
                .map(this::createProject)
                .toList();
    }

    /**
     * Find project by ID
     */
    public ProjectDto findById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return projectMapper.toDto(project);
    }

    /**
     * Find all projects
     */
    public List<ProjectDto> findAll() {
        return projectRepository.findAll()
                .stream()
                .map(projectMapper::toDto)
                .toList();
    }

    /**
     * Find projects with pagination
     */
    public Page<ProjectDto> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable)
                .map(projectMapper::toDto);
    }

    /**
     * Find projects by user ID
     */
    public List<ProjectDto> findByUserId(UUID userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        return projectRepository.findByUserId(userId)
                .stream()
                .map(projectMapper::toDto)
                .toList();
    }

    /**
     * Update project
     */
    @Transactional
    @AuditLog(action = AuditAction.PROJECT_UPDATE, targetIdExpression = "#id")
    public ProjectDto updateProject(UUID id, UpdateProjectRequest request) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        // Validate user exists if user is being updated
        if (request.userId() != null) {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new UserNotFoundException(request.userId().toString()));
            existingProject.setUser(user);
        }

        Project updated = projectMapper.partialUpdate(request, existingProject);
        Project updatedProject = projectRepository.save(updated);
        return projectMapper.toDto(updatedProject);
    }

    /**
     * Soft delete project
     */
    @Transactional
    @AuditLog(action = AuditAction.PROJECT_DELETE, targetIdExpression = "#id")
    public void deleteProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        project.setDeletedAt(OffsetDateTime.now());
        projectRepository.save(project);
    }

    /**
     * Hard delete project
     */
    @Transactional
    @AuditLog(action = AuditAction.PROJECT_DELETE, targetIdExpression = "#id", description = "Hard delete project")
    public void hardDeleteProject(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ProjectNotFoundException(id);
        }
        projectRepository.deleteById(id);
    }

    /**
     * Check if project exists
     */
    public boolean existsById(UUID id) {
        return projectRepository.existsById(id);
    }

    /**
     * Count total projects
     */
    public long count() {
        return projectRepository.count();
    }
}
