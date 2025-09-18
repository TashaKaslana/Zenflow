package org.phong.zenflow.secret.subdomain.profile.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeService;
import org.phong.zenflow.secret.subdomain.profile.dto.CreateProfileSecretsRequest;
import org.phong.zenflow.secret.subdomain.profile.dto.ProfileSecretListDto;
import org.phong.zenflow.secret.enums.SecretScope;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.secret.infrastructure.persistence.entity.SecretProfile;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretProfileRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.ProfileSecretLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.ProfileSecretLinkRepository;
import org.phong.zenflow.secret.util.AESUtil;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SecretProfileSchemaValidator validator;

    public ProfileSecretListDto createProfileSecrets(UUID workflowId, CreateProfileSecretsRequest request) {
        if (request.isDuplicated()) {
            throw new SecretDomainException("Duplicate keys found in the request.");
        }

        boolean exists = secretProfileRepository.existsByNameAndWorkflowIdAndPluginId(
                request.getName(), workflowId, request.getPluginId());
        if (exists) {
            throw new SecretDomainException("Profile with name '" + request.getName() + "' already exists for this workflow.");
        }

        SecretProfile profile = new SecretProfile();
        profile.setWorkflow(workflowRepository.getReferenceById(workflowId));
        profile.setName(request.getName());
        profile.setScope(SecretScope.WORKFLOW);
        profile.setUser(authService.getReferenceCurrentUser());
        profile.setPlugin(pluginService.findPluginById(request.getPluginId()));
        if (request.getPluginNodeId() != null) {
            profile.setPluginNode(pluginNodeService.findById(request.getPluginNodeId()));
        }
        profile = secretProfileRepository.save(profile);

        SecretProfile finalProfile = profile;
        List<Secret> secrets = request.getSecrets().entrySet().stream()
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

        boolean isValid = validator.validate(request.getPluginId(), request.getPluginNodeId(), request.getSecrets());
        if (!isValid) {
            throw new SecretDomainException("Secrets do not conform to the required schema!");
        }

        List<Secret> savedSecrets = secretRepository.saveAll(secrets);

        // Link secrets to the created profile
        var links = savedSecrets.stream().map(sec -> {
            var link = new ProfileSecretLink();
            link.setProfile(finalProfile);
            link.setSecret(sec);
            return link;
        }).collect(Collectors.toList());
        profileSecretLinkRepository.saveAll(links);

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

        return new ProfileSecretListDto(profileMap);
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
}
