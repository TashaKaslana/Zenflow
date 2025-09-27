package org.phong.zenflow.secret.subdomain.link.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.secret.subdomain.profile.service.ProfileSecretService;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowProfileBinding;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SecretLinkSyncServiceTest {

    @Mock private SecretService secretService;
    @Mock private SecretLinkService secretLinkService;
    @Mock private ProfileSecretService profileSecretService;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SecretLinkSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SecretLinkSyncService(secretService, secretLinkService, profileSecretService, workflowRepository, eventPublisher);
    }

    @Test
    void marksWorkflowInactiveWhenProfileMissing() {
        UUID workflowId = UUID.randomUUID();
        WorkflowDefinition definition = buildDefinitionWithProfile("plugin-a", "node-1");

        when(profileSecretService.resolveProfileId(any(), any(), any())).thenReturn(null);
        when(secretLinkService.getProfileLinksByWorkflowId(workflowId)).thenReturn(List.of());
        Workflow workflow = new Workflow();
        workflow.setIsActive(true);
        when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));

        syncService.syncLinksFromMetadata(workflowId, definition);

        ArgumentCaptor<Workflow> workflowCaptor = ArgumentCaptor.forClass(Workflow.class);
        verify(workflowRepository, times(1)).save(workflowCaptor.capture());
        Workflow saved = workflowCaptor.getValue();
        assertThat(saved.getIsActive()).isFalse();
        assertThat(saved.getLastValidation()).isNotNull();
        assertThat(saved.getLastValidation().getErrors()).isNotEmpty();
    }

    private WorkflowDefinition buildDefinitionWithProfile(String pluginKey, String nodeKey) {
        BaseWorkflowNode node = new BaseWorkflowNode();
        node.setKey(nodeKey);
        node.setType(NodeType.PLUGIN);
        node.setPluginNode(new PluginNodeIdentifier(pluginKey, "sample.node", "1.0.0", "builtin"));
        node.setNext(List.of());
        node.setConfig(new WorkflowConfig(Map.of(), Map.of()));
        node.setMetadata(null);
        node.setPolicy(null);

        WorkflowNodes nodes = new WorkflowNodes();
        nodes.put(node);
        var metadata = new org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata();
        metadata.profileAssignments().put(nodeKey, new WorkflowProfileBinding(pluginKey, "default"));

        return new WorkflowDefinition(nodes, metadata);
    }
}
