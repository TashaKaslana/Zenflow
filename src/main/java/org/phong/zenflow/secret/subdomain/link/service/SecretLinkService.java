package org.phong.zenflow.secret.subdomain.link.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretProfileNodeLinkInfo;
import org.phong.zenflow.secret.subdomain.profile.dto.ProfileSecretListDto;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretProfileRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.subdomain.link.dto.LinkProfileToNodeRequest;
import org.phong.zenflow.secret.subdomain.link.dto.LinkSecretToNodeRequest;
import org.phong.zenflow.secret.subdomain.link.dto.NodeProfileLinkDto;
import org.phong.zenflow.secret.subdomain.link.dto.NodeSecretLinksDto;
import org.phong.zenflow.secret.subdomain.link.dto.SecretNodeLinkInsertRequestDto;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretProfileNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretNodeLinkInfo;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.ProfileSecretLinkRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.SecretNodeLinkRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.SecretProfileNodeLinkRepository;
import org.phong.zenflow.secret.util.AESUtil;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor

public class SecretLinkService {
    private final WorkflowRepository workflowRepository;
    private final SecretRepository secretRepository;
    private final SecretNodeLinkRepository secretNodeLinkRepository;
    private final ProfileSecretLinkRepository profileSecretLinkRepository;
    private final AESUtil aesUtil;
    private final SecretProfileRepository secretProfileRepository;
    private final SecretProfileNodeLinkRepository secretProfileNodeLinkRepository;

    @Transactional(readOnly = true)
    public List<SecretNodeLinkInfo> getLinkedSecretIdsByWorkflowId(UUID workflowId) {
        return secretNodeLinkRepository.getSecretNodeLinkInfoByWorkflowId(workflowId);
    }

    @Transactional
    public void unlinkSecretByWorkflowIdAndSecretIds(Set<UUID> idList) {
        secretNodeLinkRepository.deleteAllById(idList);
    }

    @Transactional
    public void unlinkSecretsByWorkflowId(UUID workflowId) {
        secretNodeLinkRepository.deleteAllByWorkflowId(workflowId);
    }

    @Transactional
    public void linkSecretsButchToNode(UUID workflowId, List<SecretNodeLinkInsertRequestDto> links) {
        List<SecretNodeLink> newLinks = links.stream()
                .map(req -> {
                    var link = new SecretNodeLink();
                    link.setWorkflow(workflowRepository.getReferenceById(workflowId));
                    link.setSecret(secretRepository.getReferenceById(req.secretId()));
                    link.setNodeKey(req.nodeKey());
                    return link;
                })
                .toList();

        secretNodeLinkRepository.saveAll(newLinks);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> getProfilesKeyMapByWorkflowId(UUID workflowId) {
        return profileSecretLinkRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.groupingBy(
                        link -> link.getProfile().getName(),
                        Collectors.toMap(
                                link -> link.getSecret().getKey(),
                                link -> {
                                    try {
                                        return aesUtil.decrypt(link.getSecret().getEncryptedValue());
                                    } catch (Exception e) {
                                        throw new SecretDomainException("Can't decrypt value for workflowId: " + workflowId, e);
                                    }
                                },
                                (existing, replacement) -> replacement
                        )
                ));
    }

//    /**
//     * Checks whether a profile with the given name exists for a workflow and plugin key.
//     * This is used by validators to verify reference existence without exposing repository details.
//     */
//    @Transactional(readOnly = true)
//    public boolean profileExists(UUID workflowId, String pluginKey, String profileName) {
//        if (workflowId == null || pluginKey == null || profileName == null || profileName.isBlank()) {
//            return false;
//        }
//        return pluginRepository.findByKey(pluginKey)
//                .map(plugin -> secretProfileRepository.existsByNameAndWorkflowIdAndPluginId(profileName, workflowId, plugin.getId()))
//                .orElse(false);
//    }

    public void unlinkSecretFromNode(UUID workflowId, String nodeKey, UUID secretId) {
        secretNodeLinkRepository.deleteByWorkflowIdAndNodeKeyAndSecretId(workflowId, nodeKey, secretId);
    }

    public void unlinkAllSecretsFromNode(UUID workflowId, String nodeKey) {
        secretNodeLinkRepository.deleteByWorkflowIdAndNodeKey(workflowId, nodeKey);
    }

    @Transactional(readOnly = true)
    public List<UUID> getMissingSecretLinks(UUID workflowId, String nodeKey, List<UUID> expectedSecretIds) {
        if (workflowId == null || nodeKey == null || expectedSecretIds == null || expectedSecretIds.isEmpty()) {
            return List.of();
        }
        var linkedIds = secretNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .stream()
                .map(link -> link.getSecret().getId())
                .collect(Collectors.toSet());

        return expectedSecretIds.stream()
                .filter(id -> id != null && !linkedIds.contains(id))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean areSecretsLinked(UUID workflowId, String nodeKey, List<UUID> requiredSecretIds) {
        return getMissingSecretLinks(workflowId, nodeKey, requiredSecretIds).isEmpty();
    }

    public void linkProfileToNode(UUID workflowId, LinkProfileToNodeRequest request) {
        var profile = secretProfileRepository.getReferenceById(request.profileId());
        var link = secretProfileNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, request.nodeKey())
                .orElseGet(SecretProfileNodeLink::new);

        link.setWorkflow(workflowRepository.getReferenceById(workflowId));
        link.setNodeKey(request.nodeKey());
        link.setProfile(profile);
        secretProfileNodeLinkRepository.save(link);
    }

    @Transactional
    public void unlinkProfilesByWorkflowId(UUID workflowId) {
        secretProfileNodeLinkRepository.deleteAllByWorkflowId(workflowId);
    }

    @Transactional
    public void unlinkProfileLinks(Set<UUID> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return;
        }
        secretProfileNodeLinkRepository.deleteAllById(linkIds);
    }

    @Transactional
    public void linkProfilesButchToNode(UUID workflowId, List<LinkProfileToNodeRequest> links) {
        if (links == null || links.isEmpty()) {
            return;
        }

        var workflowRef = workflowRepository.getReferenceById(workflowId);
        List<SecretProfileNodeLink> newLinks = links.stream()
                .map(req -> {
                    var link = new SecretProfileNodeLink();
                    link.setWorkflow(workflowRef);
                    link.setNodeKey(req.nodeKey());
                    link.setProfile(secretProfileRepository.getReferenceById(req.profileId()));
                    return link;
                })
                .toList();

        secretProfileNodeLinkRepository.saveAll(newLinks);
    }

    public void linkSecretToNode(UUID workflowId, LinkSecretToNodeRequest request) {
        var secret = secretRepository.getReferenceById(request.secretId());
        var existing = secretNodeLinkRepository.findByWorkflowIdAndNodeKeyAndSecretId(workflowId, request.nodeKey(), request.secretId());
        if (existing.isPresent()) {
            return; // already linked
        }
        var link = new SecretNodeLink();
        link.setWorkflow(workflowRepository.getReferenceById(workflowId));
        link.setNodeKey(request.nodeKey());
        link.setSecret(secret);
        secretNodeLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public boolean isProfileLinked(UUID workflowId, String nodeKey) {
        return secretProfileNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey).isPresent();
    }

    @Transactional(readOnly = true)
    public NodeProfileLinkDto getProfileLink(UUID workflowId, String nodeKey) {
        return secretProfileNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .map(link -> new NodeProfileLinkDto(
                        workflowId,
                        nodeKey,
                        link.getProfile().getId()
                ))
                .orElse(null);
    }

    public List<SecretProfileNodeLinkInfo> getProfileLinksByWorkflowId(UUID workflowId) {
        return secretProfileNodeLinkRepository.getProfileLinksByWorkflowId(workflowId);
    }

    public void unlinkProfileFromNode(UUID workflowId, String nodeKey) {
        secretProfileNodeLinkRepository.deleteByWorkflowIdAndNodeKey(workflowId, nodeKey);
    }

    @Transactional(readOnly = true)
    public NodeSecretLinksDto getSecretLinks(UUID workflowId, String nodeKey) {
        var ids = secretNodeLinkRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .stream()
                .map(link -> link.getSecret().getId())
                .collect(Collectors.toList());
        return new NodeSecretLinksDto(workflowId, nodeKey, ids);
    }


    /**
     * Retrieves all secrets for a workflow grouped by their profile name.
     *
     * @param workflowId the workflow identifier
     * @return map keyed by profile name, each containing a map of secret key to decrypted value
     */
    @Transactional
    public ProfileSecretListDto getProfileSecretMapByWorkflowId(UUID workflowId) {
        Map<String, Map<String, String>> collect = getProfilesKeyMapByWorkflowId(workflowId);
        return new ProfileSecretListDto(collect);
    }
}
