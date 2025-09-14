package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class WorkflowValidationServiceTest {

    @Mock private SchemaValidationService schemaValidationService;
    @Mock private WorkflowDependencyValidator workflowDependencyValidator;
    @Mock private SchemaTemplateValidationService schemaTemplateValidationService;
    @Mock private PluginNodeExecutorRegistry executorRegistry;
    @Mock private org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService templateService;
    @Mock private SecretService secretService;

    private WorkflowValidationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkflowValidationService(
                schemaValidationService,
                workflowDependencyValidator,
                schemaTemplateValidationService,
                executorRegistry,
                templateService,
                secretService
        );

        // Default: no structural/template/dependency errors
        when(schemaValidationService.validateAgainstSchema(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(schemaValidationService.validateAgainstSchema(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(List.of());
        when(workflowDependencyValidator.validateNodeDependencyLoops(any())).thenReturn(List.of());
        when(schemaTemplateValidationService.validateTemplateType(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(executorRegistry.getExecutor(any())).thenReturn(java.util.Optional.empty());
        when(templateService.extractRefs(any())).thenReturn(Set.of());
    }

    private WorkflowDefinition buildDefinitionWithProfile(String pluginKey) {
        BaseWorkflowNode n = new BaseWorkflowNode();
        n.setKey("n1");
        n.setType(NodeType.PLUGIN);
        n.setPluginNode(new PluginNodeIdentifier(pluginKey, "node", "1.0.0", "executor"));
        n.setNext(List.of());
        // Include a non-empty profile section to indicate this node requires a profile
        n.setConfig(new WorkflowConfig(Map.of(), Map.of("CLIENT_ID", "")));
        n.setMetadata(null);
        n.setPolicy(null);
        return new WorkflowDefinition(new WorkflowNodes(List.of(n)), new org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata());
    }

    @Test
    void validateDefinition_profileMissing_warnsInDefinition_errorsInPublish() {
        UUID workflowId = UUID.randomUUID();
        WorkflowDefinition def = buildDefinitionWithProfile("pluginA");

        when(secretService.isProfileLinked(eq(workflowId), eq("n1"))).thenReturn(false);

        ValidationResult defRes = service.validateDefinition(workflowId, def, false);
        ValidationResult pubRes = service.validateDefinition(workflowId, def, true);

        assertThat(defRes.getErrors()).isNotEmpty();
        assertThat(pubRes.getErrors()).isNotEmpty();

        // defRes may be invalid (we use errorType distinction), pubRes must be invalid
        assertThat(pubRes.isValid()).isFalse();
    }

    @Test
    void validateDefinition_profileExists_passesBoth() {
        UUID workflowId = UUID.randomUUID();
        WorkflowDefinition def = buildDefinitionWithProfile("pluginA");
        when(secretService.isProfileLinked(eq(workflowId), eq("n1"))).thenReturn(true);

        ValidationResult defRes = service.validateDefinition(workflowId, def, false);
        ValidationResult pubRes = service.validateDefinition(workflowId, def, true);

        assertThat(defRes.isValid()).isTrue();
        assertThat(pubRes.isValid()).isTrue();
    }
}
