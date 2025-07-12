package org.phong.zenflow.project.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.project.dto.CreateProjectRequest;
import org.phong.zenflow.project.dto.ProjectDto;
import org.phong.zenflow.project.dto.UpdateProjectRequest;
import org.phong.zenflow.project.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<RestApiResponse<ProjectDto>> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectDto createdProject = projectService.createProject(request);
        return RestApiResponse.created(createdProject, "Project created successfully");
    }

    @PostMapping("/bulk")
    public ResponseEntity<RestApiResponse<List<ProjectDto>>> createProjects(@Valid @RequestBody List<CreateProjectRequest> requests) {
        List<ProjectDto> createdProjects = projectService.createProjects(requests);
        return RestApiResponse.created(createdProjects, "Projects created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<ProjectDto>> getProjectById(@PathVariable UUID id) {
        ProjectDto project = projectService.findById(id);
        return RestApiResponse.success(project, "Project retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<ProjectDto>>> getAllProjects() {
        List<ProjectDto> projects = projectService.findAll();
        return RestApiResponse.success(projects, "Projects retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<ProjectDto>>> getAllProjectsPaginated(Pageable pageable) {
        Page<ProjectDto> projects = projectService.findAll(pageable);
        return RestApiResponse.success(projects, "Projects retrieved successfully");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<RestApiResponse<List<ProjectDto>>> getProjectsByUserId(@PathVariable UUID userId) {
        List<ProjectDto> projects = projectService.findByUserId(userId);
        return RestApiResponse.success(projects, "User projects retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<ProjectDto>> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectDto updatedProject = projectService.updateProject(id, request);
        return RestApiResponse.success(updatedProject, "Project updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return RestApiResponse.noContent();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<RestApiResponse<Void>> hardDeleteProject(@PathVariable UUID id) {
        projectService.hardDeleteProject(id);
        return RestApiResponse.noContent();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkProjectExists(@PathVariable UUID id) {
        boolean exists = projectService.existsById(id);
        return RestApiResponse.success(exists, "Project existence checked");
    }

    @GetMapping("/count")
    public ResponseEntity<RestApiResponse<Long>> getProjectCount() {
        long count = projectService.count();
        return RestApiResponse.success(count, "Project count retrieved successfully");
    }
}
