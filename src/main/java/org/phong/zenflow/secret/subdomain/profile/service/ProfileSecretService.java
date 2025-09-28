package org.phong.zenflow.secret.subdomain.profile.service;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeService;
import org.phong.zenflow.secret.enums.SecretScope;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretProfileRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.ProfileSecretLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.ProfileSecretLinkRepository;
import org.phong.zenflow.secret.subdomain.profile.dto.CreateProfileSecretsRequest;
import org.phong.zenflow.secret.subdomain.profile.dto.ProfilePreparationResult;
import org.phong.zenflow.secret.subdomain.profile.dto.ProfileSecretCreationResult;
import org.phong.zenflow.secret.subdomain.profile.dto.ProfileSecretListDto;
import org.phong.zenflow.secret.subdomain.profile.entity.SecretProfile;
import org.phong.zenflow.secret.util.AESUtil;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProfileSecretService {
    private final SecretRepository secretRepository;
    private final SecretProfileRepository secretProfileRepository;
    private final ProfileSecretLinkRepository profileSecretLinkRepository;
    private final WorkflowRepository workflowRepository;
    private final PluginService pluginService;
    private final PluginNodeService pluginNodeService;
    private final AuthService authService;
    private final AESUtil aesUtil;
    private final ProfilePreparationService profilePreparationService;

    @Transactional
    public ProfileSecretCreationResult createProfileSecrets(UUID workflowId, CreateProfileSecretsRequest request) {
        if (request.isDuplicated()) {
            throw new SecretDomainException("Duplicate keys found in the request.");
        }

        boolean exists = secretProfileRepository.existsByNameAndWorkflowIdAndPluginId(
                request.getName(), workflowId, request.getPluginId());
        if (exists) {
            throw new SecretDomainException("Profile with name '" + request.getName() + "' already exists for this workflow.");
        }

        ProfilePreparationResult preparationResult = profilePreparationService.prepareSecrets(
                request.getPluginId(),
                request.getPluginNodeId(),
                request.getSecrets(),
                request.getCallbackUrl()
        );

        if (!preparationResult.isReady()) {
            return ProfileSecretCreationResult.pending(preparationResult.pendingRequests());
        }

        SecretProfile finalProfile = saveProfileSecret(workflowId, request);
        List<Secret> savedSecrets = saveSecrets(preparationResult.preparedSecrets(), finalProfile);
        linkSecretsToProfile(savedSecrets, finalProfile);

        Map<String, Map<String, String>> profileMap = Map.of(
                finalProfile.getName(),
                savedSecrets.stream().collect(Collectors.toMap(
                        Secret::getKey,
                        s -> {
                            try {
                                return aesUtil.decrypt(s.getEncryptedValue());
                            } catch (Exception e) {
                                throw new SecretDomainException("Can't decrypt value for workflowId: " + workflowId, e);
                            }
                        }
                ))
        );

        return ProfileSecretCreationResult.completed(new ProfileSecretListDto(profileMap));
    }

    @NotNull
    private List<Secret> saveSecrets(Map<String, String> secrets, SecretProfile finalProfile) {
        List<Secret> secretList = secrets.entrySet().stream()
                .map(entry -> {
                    Secret secret = new Secret();
                    secret.setWorkflow(finalProfile.getWorkflow());
                    secret.setKey(entry.getKey());
                    secret.setScope(SecretScope.WORKFLOW);

                    try {
                        secret.setEncryptedValue(aesUtil.encrypt(entry.getValue()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    secret.setUser(authService.getReferenceCurrentUser());
                    return secret;
                })
                .collect(Collectors.toList());

        return secretRepository.saveAll(secretList);
    }

    private void linkSecretsToProfile(List<Secret> savedSecrets, SecretProfile finalProfile) {
        var links = savedSecrets.stream().map(sec -> {
            var link = new ProfileSecretLink();
            link.setProfile(finalProfile);
            link.setSecret(sec);
            return link;
        }).collect(Collectors.toList());
        profileSecretLinkRepository.saveAll(links);
    }

    @NotNull
    private SecretProfile saveProfileSecret(UUID workflowId, CreateProfileSecretsRequest request) {
        SecretProfile profile = new SecretProfile();
        profile.setWorkflow(workflowRepository.getReferenceById(workflowId));
        profile.setName(request.getName());
        profile.setScope(SecretScope.WORKFLOW);
        profile.setUser(authService.getReferenceCurrentUser());
        profile.setPlugin(pluginService.findPluginById(request.getPluginId()));
        if (request.getPluginNodeId() != null) {
            profile.setPluginNode(pluginNodeService.findById(request.getPluginNodeId()));
        }

        return secretProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public UUID resolveProfileId(UUID workflowId, String pluginKey, String profileName) {
        if (workflowId == null || pluginKey == null || profileName == null || profileName.isBlank()) {
            return null;
        }
        return pluginService.findByKey(pluginKey)
                .flatMap(plugin -> secretProfileRepository.findByNameAndWorkflowIdAndPluginId(profileName, workflowId, plugin.getId()))
                .map(SecretProfile::getId)
                .orElse(null);
    }

    // Returns a map of pluginKey -> (profileName -> profileId) for all profiles in the given workflow
    @Transactional(readOnly = true)
    public Map<String, Map<String, UUID>> getPluginProfileMap(UUID workflowId) {
        List<SecretProfile> profiles = secretProfileRepository.findByWorkflowId(workflowId);
        Map<String, Map<String, UUID>> profileMap = new HashMap<>();

        for (SecretProfile profile : profiles) {
            String pluginKey = profile.getPlugin().getKey();
            profileMap.putIfAbsent(pluginKey, new HashMap<>());
            profileMap.get(pluginKey).put(profile.getName(), profile.getId());
        }

        return profileMap;
    }
}
