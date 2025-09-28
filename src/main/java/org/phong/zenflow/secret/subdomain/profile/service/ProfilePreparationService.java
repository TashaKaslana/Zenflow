package org.phong.zenflow.secret.subdomain.profile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptorRegistry;
import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationRequest;
import org.phong.zenflow.plugin.subdomain.registry.profile.RegisteredPluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.subdomain.profile.dto.ProfilePreparationCallbackPayload;
import org.phong.zenflow.secret.subdomain.profile.dto.ProfilePreparationResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilePreparationService {

    private final SecretProfileSchemaValidator schemaValidator;
    private final PluginProfileDescriptorRegistry profileDescriptorRegistry;
    private final SchemaRegistry schemaRegistry;
    private final WebClient webClient;

    public ProfilePreparationResult prepareSecrets(
            UUID pluginId,
            UUID pluginNodeId,
            Map<String, String> submittedSecrets,
            String callbackUrl
    ) {
        Map<String, String> normalized = submittedSecrets == null ? Map.of() : Map.copyOf(submittedSecrets);

        if (!schemaValidator.validate(pluginId, pluginNodeId, normalized)) {
            throw new SecretDomainException("Secrets do not conform to the required schema!");
        }

        if (pluginId == null) {
            return new ProfilePreparationResult(new LinkedHashMap<>(normalized), List.of());
        }

        List<RegisteredPluginProfileDescriptor> descriptors = profileDescriptorRegistry.getByPluginId(pluginId);
        List<RegisteredPluginProfileDescriptor> relevantDescriptors = resolveRelevantDescriptors(
                descriptors,
                pluginNodeId,
                normalized.keySet()
        );

        if (relevantDescriptors.isEmpty()) {
            return new ProfilePreparationResult(new LinkedHashMap<>(normalized), List.of());
        }

        DefaultProfilePreparationContext context = new DefaultProfilePreparationContext(normalized, Map.of());
        for (RegisteredPluginProfileDescriptor descriptor : relevantDescriptors) {
            if (!descriptor.descriptor().requiresPreparation()) {
                continue;
            }
            descriptor.descriptor().prepareProfile(context);
        }

        Map<String, String> prepared = new LinkedHashMap<>(normalized);
        for (String fieldKey : context.discardedFields()) {
            prepared.remove(fieldKey);
        }
        prepared.putAll(context.generatedSecrets());

        List<ProfilePreparationRequest> pending = new ArrayList<>(context.pendingRequests());
        if (!pending.isEmpty()) {
            dispatchCallback(callbackUrl, pluginId, pluginNodeId, prepared, pending);
        }

        return new ProfilePreparationResult(prepared, pending);
    }

    private void dispatchCallback(
            String callbackUrl,
            UUID pluginId,
            UUID pluginNodeId,
            Map<String, String> prepared,
            List<ProfilePreparationRequest> pending
    ) {
        if (!StringUtils.hasText(callbackUrl)) {
            log.warn("Profile preparation pending but no callback URL provided. Plugin: {}", pluginId);
            return;
        }

        ProfilePreparationCallbackPayload payload = new ProfilePreparationCallbackPayload(
                pluginId,
                pluginNodeId,
                prepared,
                pending
        );

        webClient.post()
                .uri(callbackUrl)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .doOnError(error -> log.error("Failed to notify profile preparation callback URL {}", callbackUrl, error))
                .onErrorResume(error -> Mono.empty())
                .subscribe();
    }

    private List<RegisteredPluginProfileDescriptor> resolveRelevantDescriptors(
            List<RegisteredPluginProfileDescriptor> descriptors,
            UUID pluginNodeId,
            Set<String> submittedKeys
    ) {
        if (descriptors == null || descriptors.isEmpty()) {
            return List.of();
        }

        List<RegisteredPluginProfileDescriptor> fromNode = descriptorsFromNode(descriptors, pluginNodeId);
        if (!fromNode.isEmpty()) {
            return fromNode;
        }

        if (submittedKeys != null && !submittedKeys.isEmpty()) {
            List<RegisteredPluginProfileDescriptor> matched = descriptors.stream()
                    .filter(d -> d.descriptor().requiresPreparation())
                    .filter(d -> descriptorMatchesSubmittedKeys(d, submittedKeys))
                    .toList();
            if (!matched.isEmpty()) {
                return matched;
            }
        }

        return descriptors.stream()
                .filter(d -> d.descriptor().requiresPreparation())
                .toList();
    }

    private List<RegisteredPluginProfileDescriptor> descriptorsFromNode(
            List<RegisteredPluginProfileDescriptor> descriptors,
            UUID pluginNodeId
    ) {
        if (pluginNodeId == null) {
            return List.of();
        }
        try {
            JSONObject schema = schemaRegistry.getSchemaByTemplateString(pluginNodeId.toString());
            List<String> profileKeys = schemaValidator.getRequiredProfileKeys(schema);
            if (profileKeys == null || profileKeys.isEmpty()) {
                return List.of();
            }
            return descriptors.stream()
                    .filter(d -> d.descriptor().requiresPreparation())
                    .filter(d -> profileKeys.contains(d.descriptor().id()))
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to inspect node schema while resolving profile descriptors", ex);
            return List.of();
        }
    }

    private boolean descriptorMatchesSubmittedKeys(
            RegisteredPluginProfileDescriptor descriptor,
            Set<String> submittedKeys
    ) {
        Object propertiesObj = descriptor.schema().get("properties");
        if (!(propertiesObj instanceof Map<?, ?> properties)) {
            return false;
        }
        for (Object keyObj : properties.keySet()) {
            if (keyObj != null && submittedKeys.contains(keyObj.toString())) {
                return true;
            }
        }
        return false;
    }
}
