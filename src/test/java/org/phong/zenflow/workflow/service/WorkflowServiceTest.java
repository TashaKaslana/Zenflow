package org.phong.zenflow.workflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.phong.zenflow.project.infrastructure.persistence.repository.ProjectRepository;
import org.phong.zenflow.workflow.dto.UpsertWorkflowDefinition;
import org.phong.zenflow.workflow.dto.WorkflowDefinitionChangeRequest;
import org.phong.zenflow.workflow.infrastructure.mapstruct.WorkflowMapper;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.service.event.WorkflowDefinitionUpdatedEvent;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.services.WorkflowDefinitionService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.phong.zenflow.workflow.service.dto.WorkflowDefinitionUpdateResult;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowServiceTest {

    @Mock private WorkflowRepository workflowRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private WorkflowMapper workflowMapper;
    @Mock private WorkflowDefinitionService definitionService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private WorkflowService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkflowService(
                workflowRepository,
                projectRepository,
                workflowMapper,
                definitionService,
                eventPublisher
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
        when(workflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ValidationResult strictResult = new ValidationResult("publish", new ArrayList<>());
        when(definitionService.buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id)))
                .thenReturn(strictResult);

        WorkflowDefinitionUpdateResult result = service.updateWorkflowDefinition(id, null, true);

        assertThat(result.definition()).isSameAs(wf.getDefinition());
        assertThat(result.validation()).isSameAs(strictResult);
        assertThat(result.isActive()).isTrue();
        verify(definitionService).buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id));

        ArgumentCaptor<WorkflowDefinitionUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowDefinitionUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().definition()).isSameAs(wf.getDefinition());
    }

    @Test
    void updateWorkflowDefinition_publishFail_setsInactiveButSaves() {
        UUID id = UUID.randomUUID();
        Workflow wf = existingWorkflow(id);
        when(workflowRepository.findById(id)).thenReturn(Optional.of(wf));
        when(workflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ValidationError error = ValidationError.builder()
                .nodeKey("node")
                .errorType("definition")
                .errorCode(ValidationErrorCode.INVALID_REFERENCE)
                .path("metadata.secrets")
                .message("missing secret")
                .build();
        ValidationResult strictResult = new ValidationResult("publish", List.of(error));
        when(definitionService.buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id)))
                .thenReturn(strictResult);

        WorkflowDefinitionUpdateResult result = service.updateWorkflowDefinition(id, null, true);

        assertThat(result.definition()).isSameAs(wf.getDefinition());
        assertThat(result.validation()).isSameAs(strictResult);
        assertThat(result.validation()).isSameAs(strictResult);
        assertThat(result.validation().getErrors()).contains(error);
        assertThat(result.isActive()).isFalse();
        verify(definitionService).buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id));
        verify(eventPublisher).publishEvent(any(WorkflowDefinitionUpdatedEvent.class));
    }

    @Test
    void updateWorkflowDefinition_noPublish_keepsActiveAndSkipsValidation() {
        UUID id = UUID.randomUUID();
        Workflow wf = existingWorkflow(id);
        wf.setIsActive(true);
        when(workflowRepository.findById(id)).thenReturn(Optional.of(wf));
        when(workflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ValidationResult validation = new ValidationResult("definition", new ArrayList<>());
        when(definitionService.buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id)))
                .thenReturn(validation);

        WorkflowDefinitionUpdateResult result = service.updateWorkflowDefinition(id, null, false);

        assertThat(result.definition()).isSameAs(wf.getDefinition());
        assertThat(result.validation()).isSameAs(validation);
        assertThat(result.isActive()).isTrue();
        verify(definitionService).buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id));
        verify(eventPublisher).publishEvent(any(WorkflowDefinitionUpdatedEvent.class));
    }

    @Test
    void updateWorkflowDefinition_mutationAggregatesErrorsButPersists() {
        UUID id = UUID.randomUUID();
        Workflow wf = existingWorkflow(id);
        when(workflowRepository.findById(id)).thenReturn(Optional.of(wf));
        WorkflowDefinitionChangeRequest request = new WorkflowDefinitionChangeRequest(
                new UpsertWorkflowDefinition(new WorkflowNodes(), new WorkflowMetadata()),
                List.of("obsolete-node")
        );
        when(definitionService.removeNodesWithoutValidation(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ValidationError aggregated = ValidationError.builder()
                .nodeKey("bad-node")
                .errorType("definition")
                .errorCode(ValidationErrorCode.VALIDATION_ERROR)
                .path("nodes.key")
                .message("reserved prefix")
                .build();
        when(definitionService.upsertWithoutValidation(any(), any())).thenReturn(List.of(aggregated));
        ValidationResult baseResult = new ValidationResult("compose", new ArrayList<>());
        when(definitionService.buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id)))
                .thenReturn(baseResult);
        when(workflowRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowDefinitionUpdateResult result = service.updateWorkflowDefinition(id, request, false);

        assertThat(result.definition()).isSameAs(wf.getDefinition());
        assertThat(result.validation()).isSameAs(baseResult);
        assertThat(result.validation().getErrors()).contains(aggregated);
        verify(definitionService).removeNodesWithoutValidation(any(), any());
        verify(definitionService).upsertWithoutValidation(any(), any());
        verify(definitionService).buildStaticContextAndValidate(any(WorkflowDefinition.class), eq(id));
        verify(eventPublisher).publishEvent(any(WorkflowDefinitionUpdatedEvent.class));
    }
}
