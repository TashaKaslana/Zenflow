package org.phong.zenflow.workflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.services.WorkflowDefinitionService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.phong.zenflow.workflow.subdomain.trigger.services.WorkflowTriggerService;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowServiceTest {

    @Mock private WorkflowRepository workflowRepository;
    @Mock private org.phong.zenflow.project.infrastructure.persistence.repository.ProjectRepository projectRepository;
    @Mock private org.phong.zenflow.workflow.infrastructure.mapstruct.WorkflowMapper workflowMapper;
    @Mock private WorkflowDefinitionService definitionService;
    @Mock private WorkflowTriggerService triggerService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private WorkflowValidationService validationService;
    @Mock private org.phong.zenflow.secret.service.SecretService secretService;
    @Mock private org.phong.zenflow.secret.service.SecretLinkSyncService linkSyncService;

    private WorkflowService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkflowService(
                workflowRepository,
                projectRepository,
                workflowMapper,
                definitionService,
                triggerService,
                eventPublisher,
                validationService,
                linkSyncService
        );
    }

    private Workflow existingWorkflow(UUID id) {
        Workflow wf = new Workflow();
        wf.setId(id);
        wf.setName("wf");
        wf.setProject(new Project());
        wf.setIsActive(false);
        wf.setDefinition(new WorkflowDefinition());
        return wf;
    }

    @Test
    void updateWorkflowDefinition_publishSuccess_setsActiveTrue() {
        UUID id = UUID.randomUUID();
        Workflow wf = existingWorkflow(id);
        when(workflowRepository.findById(id)).thenReturn(Optional.of(wf));
        when(definitionService.removeNodes(any(), any())).thenAnswer(i -> i.getArgument(0));
        when(definitionService.upsert(any(), any())).thenAnswer(i -> i.getArgument(1));
        when(validationService.validateDefinition(eq(id), any(), eq(true))).thenReturn(new ValidationResult("publish", java.util.List.of()));
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateWorkflowDefinition(id, null, true);

        assertThat(wf.getIsActive()).isTrue();
        verify(triggerService).synchronizeTrigger(eq(id), any());
    }

    @Test
    void updateWorkflowDefinition_publishFail_setsActiveFalse() {
        UUID id = UUID.randomUUID();
        Workflow wf = existingWorkflow(id);
        when(workflowRepository.findById(id)).thenReturn(Optional.of(wf));
        when(definitionService.removeNodes(any(), any())).thenAnswer(i -> i.getArgument(0));
        when(definitionService.upsert(any(), any())).thenAnswer(i -> i.getArgument(1));
        org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError err =
                org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError.builder()
                        .nodeKey("n")
                        .errorType("definition")
                        .errorCode(org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode.INVALID_REFERENCE)
                        .path("p")
                        .message("m")
                        .build();
        ValidationResult fail = new ValidationResult("publish", java.util.List.of(err));
        when(validationService.validateDefinition(eq(id), any(), eq(true))).thenReturn(fail);
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateWorkflowDefinition(id, null, true);

        assertThat(wf.getIsActive()).isFalse();
        verify(triggerService).synchronizeTrigger(eq(id), any());
    }

    @Test
    void updateWorkflowDefinition_noPublish_keepsActive() {
        UUID id = UUID.randomUUID();
        Workflow wf = existingWorkflow(id);
        wf.setIsActive(true);
        when(workflowRepository.findById(id)).thenReturn(Optional.of(wf));
        when(definitionService.removeNodes(any(), any())).thenAnswer(i -> i.getArgument(0));
        when(definitionService.upsert(any(), any())).thenAnswer(i -> i.getArgument(1));
        when(workflowRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateWorkflowDefinition(id, null, false);

        assertThat(wf.getIsActive()).isTrue();
        verify(triggerService).synchronizeTrigger(eq(id), any());
        verify(validationService, never()).validateDefinition(eq(id), any(), anyBoolean());
    }
}
