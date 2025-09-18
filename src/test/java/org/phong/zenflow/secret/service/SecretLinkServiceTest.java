package org.phong.zenflow.secret.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.secret.subdomain.link.dto.LinkProfileToNodeRequest;
import org.phong.zenflow.secret.subdomain.link.dto.LinkSecretToNodeRequest;
import org.phong.zenflow.secret.subdomain.link.dto.NodeProfileLinkDto;
import org.phong.zenflow.secret.subdomain.link.dto.NodeSecretLinksDto;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretNodeLink;
import org.phong.zenflow.secret.infrastructure.persistence.entity.SecretProfile;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretProfileNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.SecretNodeLinkRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.SecretProfileNodeLinkRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretProfileRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.subdomain.link.service.SecretLinkService;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Secret link operations")
class SecretLinkServiceTest {

    @Mock private SecretRepository secretRepository;
    @Mock private SecretProfileRepository secretProfileRepository;
    @Mock private SecretProfileNodeLinkRepository secretProfileNodeLinkRepository;
    @Mock private SecretNodeLinkRepository secretNodeLinkRepository;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private SecretLinkService secretLinkService;

    @Test
    void linkProfileToNode_createsOrUpdates() {
        UUID workflowId = UUID.randomUUID();
        String nodeKey = "email";
        UUID profileId = UUID.randomUUID();

        SecretProfile profile = new SecretProfile();
        profile.setId(profileId);

        when(secretProfileRepository.getReferenceById(profileId)).thenReturn(profile);
        when(secretProfileNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey))
                .thenReturn(Optional.empty());
        when(workflowRepository.getReferenceById(workflowId)).thenReturn(new org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow());

        secretLinkService.linkProfileToNode(workflowId, new LinkProfileToNodeRequest(profileId, nodeKey));

        ArgumentCaptor<SecretProfileNodeLink> captor = ArgumentCaptor.forClass(SecretProfileNodeLink.class);
        verify(secretProfileNodeLinkRepository).save(captor.capture());
        SecretProfileNodeLink saved = captor.getValue();
        assertEquals(nodeKey, saved.getNodeKey());
        assertEquals(profileId, saved.getProfile().getId());
    }

    @Test
    void getProfileLink_found() {
        UUID workflowId = UUID.randomUUID();
        String nodeKey = "email";
        UUID profileId = UUID.randomUUID();

        SecretProfile profile = new SecretProfile();
        profile.setId(profileId);
        SecretProfileNodeLink link = new SecretProfileNodeLink();
        link.setProfile(profile);

        when(secretProfileNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey))
                .thenReturn(Optional.of(link));

        NodeProfileLinkDto dto = secretLinkService.getProfileLink(workflowId, nodeKey);
        assertNotNull(dto);
        assertEquals(workflowId, dto.workflowId());
        assertEquals(nodeKey, dto.nodeKey());
        assertEquals(profileId, dto.profileId());
    }

    @Test
    void unlinkProfileFromNode_callsRepo() {
        UUID workflowId = UUID.randomUUID();
        String nodeKey = "email";
        secretLinkService.unlinkProfileFromNode(workflowId, nodeKey);
        verify(secretProfileNodeLinkRepository).deleteByWorkflowIdAndNodeKey(workflowId, nodeKey);
    }

    @Test
    void linkSecretToNode_idempotentAndCreate() {
        UUID workflowId = UUID.randomUUID();
        String nodeKey = "email";
        UUID secretId = UUID.randomUUID();

        when(secretNodeLinkRepository.findByWorkflowIdAndNodeKeyAndSecretId(workflowId, nodeKey, secretId))
                .thenReturn(Optional.empty());

        Secret secret = new Secret();
        secret.setId(secretId);
        when(secretRepository.getReferenceById(secretId)).thenReturn(secret);
        org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow wf = new org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow();
        when(workflowRepository.getReferenceById(workflowId)).thenReturn(wf);

        secretLinkService.linkSecretToNode(workflowId, new LinkSecretToNodeRequest(secretId, nodeKey));
        verify(secretNodeLinkRepository).save(any(SecretNodeLink.class));

        // second call should be no-op save
        when(secretNodeLinkRepository.findByWorkflowIdAndNodeKeyAndSecretId(workflowId, nodeKey, secretId))
                .thenReturn(Optional.of(new SecretNodeLink()));
        secretLinkService.linkSecretToNode(workflowId, new LinkSecretToNodeRequest(secretId, nodeKey));
        verify(secretNodeLinkRepository, times(1)).save(any(SecretNodeLink.class));
    }

    @Test
    void getSecretLinks_returnsIds() {
        UUID workflowId = UUID.randomUUID();
        String nodeKey = "email";
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();

        Secret sec1 = new Secret(); sec1.setId(s1);
        Secret sec2 = new Secret(); sec2.setId(s2);
        SecretNodeLink l1 = new SecretNodeLink(); l1.setSecret(sec1);
        SecretNodeLink l2 = new SecretNodeLink(); l2.setSecret(sec2);

        when(secretNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey))
                .thenReturn(List.of(l1, l2));

        NodeSecretLinksDto dto = secretLinkService.getSecretLinks(workflowId, nodeKey);
        assertNotNull(dto);
        assertEquals(workflowId, dto.workflowId());
        assertEquals(nodeKey, dto.nodeKey());
        assertEquals(List.of(s1, s2), dto.secretIds());
    }

    @Test
    void unlinkSecret_callsRepo() {
        UUID workflowId = UUID.randomUUID();
        String nodeKey = "email";
        UUID secretId = UUID.randomUUID();

        secretLinkService.unlinkSecretFromNode(workflowId, nodeKey, secretId);
        verify(secretNodeLinkRepository).deleteByWorkflowIdAndNodeKeyAndSecretId(workflowId, nodeKey, secretId);

        secretLinkService.unlinkAllSecretsFromNode(workflowId, nodeKey);
        verify(secretNodeLinkRepository).deleteByWorkflowIdAndNodeKey(workflowId, nodeKey);
    }
}

