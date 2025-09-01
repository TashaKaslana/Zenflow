package org.phong.zenflow.setup;

import org.phong.zenflow.project.dto.CreateProjectRequest;
import org.phong.zenflow.project.dto.ProjectDto;
import org.phong.zenflow.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ProjectSetupTest {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private TestDataConfiguration testDataConfig;

    @SuppressWarnings("unchecked")
    public void setupProjects() {
        Map<String, UUID> userMap = (Map<String, UUID>) ContextSetupHolder.get("users");

        if (userMap == null || userMap.isEmpty()) {
            throw new IllegalStateException("Users must be set up before projects");
        }

        // Use configuration data with dynamic project assignment
        List<CreateProjectRequest> projects = testDataConfig.getProjects().getDescriptions().entrySet().stream()
                .map(entry -> {
                    String projectName = entry.getKey();
                    String description = entry.getValue();
                    // Assign alpha to admin, beta to user, others to admin by default
                    UUID ownerId = "alpha".equals(projectName) ? userMap.get("admin") : userMap.get("user");
                    return new CreateProjectRequest(ownerId, projectName, description);
                })
                .toList();

        List<ProjectDto> dtoList = projectService.createProjects(projects);
        Map<String, UUID> projectIdMap = dtoList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        dto -> projects.stream()
                                .filter(req -> req.name().equals(dto.name()))
                                .findFirst()
                                .map(CreateProjectRequest::name)
                                .orElse("unknown"),
                        ProjectDto::id
                ));

        ContextSetupHolder.set("projects", projectIdMap);
    }
}
