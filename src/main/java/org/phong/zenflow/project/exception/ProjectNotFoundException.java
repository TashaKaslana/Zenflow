package org.phong.zenflow.project.exception;

import java.util.UUID;

public class ProjectNotFoundException extends ProjectDomainException {
    public ProjectNotFoundException(UUID id) {
        super("Project with ID " + id + " not found.");
    }
}
