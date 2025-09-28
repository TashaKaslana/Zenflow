package org.phong.zenflow.plugin.subdomain.registry.profile;

import java.util.Map;

/**
 * Context exposed to {@link PluginProfileDescriptor} during profile preparation.
 * Implementations are provided by the runtime when additional setup is required.
 */
public interface ProfilePreparationContext {

    /**
     * @return immutable view of values submitted by the user via the profile form.
     */
    Map<String, String> submittedValues();

    /**
     * Records a generated secret that should be persisted with the profile.
     * Implementations may encrypt the value before storage.
     */
    void putGeneratedSecret(String key, String value);

    /**
     * Access a previously stored generated secret by key if the preparation process runs multiple times.
     */
    default String getGeneratedSecret(String key) {
        return null;
    }

    /**
     * Request additional input from the user before preparation can finish. The runtime
     * should surface the request and collect the value in a follow-up submission.
     */
    default void requestAdditionalField(ProfilePreparationRequest request) {
    }

    /**
     * Mark a previously requested field as satisfied so the runtime can clear pending prompts.
     */
    default void clearRequestedField(String fieldKey) {
    }
}
