package org.phong.zenflow.secret.subdomain.profile.service;

import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationContext;
import org.phong.zenflow.plugin.subdomain.registry.profile.ProfilePreparationRequest;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default runtime implementation of {@link ProfilePreparationContext} used by the secret service layer.
 */
final class DefaultProfilePreparationContext implements ProfilePreparationContext {

    private final Map<String, String> submittedValues;
    private final Map<String, String> existingGeneratedSecrets;
    private final Map<String, String> generatedSecrets = new LinkedHashMap<>();
    private final Map<String, ProfilePreparationRequest> pendingRequests = new LinkedHashMap<>();
    private final Set<String> discardedFields = new LinkedHashSet<>();

    DefaultProfilePreparationContext(Map<String, String> submittedValues,
                                     Map<String, String> existingGeneratedSecrets) {
        this.submittedValues = submittedValues == null ? Map.of() : Map.copyOf(submittedValues);
        this.existingGeneratedSecrets = existingGeneratedSecrets == null ? Map.of() : Map.copyOf(existingGeneratedSecrets);
    }

    @Override
    public Map<String, String> submittedValues() {
        return submittedValues;
    }

    @Override
    public void putGeneratedSecret(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Generated secret key cannot be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Generated secret value cannot be null");
        }
        generatedSecrets.put(key, value);
    }

    @Override
    public String getGeneratedSecret(String key) {
        if (key == null) {
            return null;
        }
        String generated = generatedSecrets.get(key);
        if (generated != null) {
            return generated;
        }
        return existingGeneratedSecrets.get(key);
    }

    @Override
    public void requestAdditionalField(ProfilePreparationRequest request) {
        if (request == null) {
            return;
        }
        pendingRequests.put(request.fieldKey(), request);
    }

    @Override
    public void clearRequestedField(String fieldKey) {
        if (fieldKey == null) {
            return;
        }
        pendingRequests.remove(fieldKey);
        discardedFields.add(fieldKey);
    }

    Map<String, String> generatedSecrets() {
        return generatedSecrets;
    }

    Collection<ProfilePreparationRequest> pendingRequests() {
        return pendingRequests.values();
    }

    Set<String> discardedFields() {
        return discardedFields;
    }
}
