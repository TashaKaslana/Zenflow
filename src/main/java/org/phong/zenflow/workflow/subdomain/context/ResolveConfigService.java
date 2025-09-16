package org.phong.zenflow.workflow.subdomain.context;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.secret.dto.AggregatedSecretSetupDto;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResolveConfigService {
    private final TemplateService templateService;
    private final SecretService secretService;

    /**
     * Resolves workflow config by handling zenflow.secrets.* and zenflow.profiles.* templates
     * during the definition phase.
     */
    public WorkflowConfig resolveConfig(WorkflowConfig config, UUID workflowId, String nodeKey) {
        if (config == null || config.input() == null) {
            return config;
        }

        Map<String, Object> resolvedInput = resolveMap(config.input(), workflowId, nodeKey);
        return new WorkflowConfig(resolvedInput, config.output(), config.profile());
    }

    private Map<String, Object> resolveMap(Map<String, Object> map, UUID workflowId, String nodeKey) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> resolveValue(e.getValue(), workflowId, nodeKey)));
    }

    private Object resolveValue(Object value, UUID workflowId, String nodeKey) {
        if (value instanceof String str) {
            if (templateService != null && templateService.isTemplate(str)) {
                return resolveTemplate(str, workflowId, nodeKey);
            }
            return str;
        } else if (value instanceof Map<?, ?> m) {
            return resolveMap(ObjectConversion.convertObjectToMap(m), workflowId, nodeKey);
        } else if (value instanceof List<?> list) {
            return list.stream().map(item -> resolveValue(item, workflowId, nodeKey)).toList();
        }

        return value;
    }

    private Object resolveTemplate(String template, UUID workflowId, String nodeKey) {
        // If workflowId is null, we can't resolve secrets/profiles, return as-is
        if (workflowId == null || nodeKey == null) {
            return template;
        }

        // Extract references to check if this is a reserved key template
        Set<String> refs = templateService.extractRefs(template);
        if (refs.isEmpty()) {
            return template;
        }

        // Check if any reference starts with zenflow.secrets or zenflow.profiles
        boolean hasReservedKeys = refs.stream().anyMatch(ref ->
            ref.startsWith("zenflow.secrets.") || ref.startsWith("zenflow.profiles."));

        if (!hasReservedKeys) {
            return template; // Not a reserved key template, return as-is
        }

        // Resolve using definition-phase resolution
        return resolveDefinitionPhaseTemplate(template, workflowId, nodeKey);
    }

    private Object resolveDefinitionPhaseTemplate(String template, UUID workflowId, String nodeKey) {
        try {
            // Get aggregated secrets and profiles data
            AggregatedSecretSetupDto agg = secretService.getAggregatedSecretsProfilesAndNodeIndex(workflowId);

            // Create a definition-phase resolver
            DefinitionPhaseResolver resolver = new DefinitionPhaseResolver(agg, nodeKey);

            return templateService.resolveDefinitionPhase(template, workflowId, nodeKey, resolver);
        } catch (Exception e) {
            // If resolution fails, return the original template
            return template;
        }
    }

    /**
     * Internal class that provides definition-phase resolution for secrets and profiles
     */
    private static class DefinitionPhaseResolver implements TemplateService.ReservedValueResolver {
        private final AggregatedSecretSetupDto aggregatedData;
        private final String nodeKey;

        public DefinitionPhaseResolver(AggregatedSecretSetupDto aggregatedData, String nodeKey) {
            this.aggregatedData = aggregatedData;
            this.nodeKey = nodeKey;
        }

        /**
         * Resolves secret value by key for the current workflow
         */
        @Override
        public String resolveSecretValue(UUID workflowId, String secretKey) {
            // Find secret by key from nodeSecrets mapping
            List<String> nodeSecretIds = aggregatedData.nodeSecrets().get(nodeKey);
            if (nodeSecretIds == null) {
                return null;
            }

            // Find the secret ID that matches the requested key
            for (String secretId : nodeSecretIds) {
                String actualSecretKey = aggregatedData.secretKeys().get(secretId);
                if (secretKey.equals(actualSecretKey)) {
                    return aggregatedData.secrets().get(secretId);
                }
            }

            return null;
        }

        /**
         * Resolves profile value by field name for the current node
         */
        @Override
        public String resolveProfileValue(UUID workflowId, String nodeKey, String profileField) {
            // Get the profile ID for this node
            String profileId = aggregatedData.nodeProfiles().get(nodeKey);
            if (profileId == null) {
                return null;
            }

            // Get the profile's secrets map
            Map<String, String> profileSecrets = aggregatedData.profiles().get(profileId);
            if (profileSecrets == null) {
                return null;
            }

            // Return the requested field value
            return profileSecrets.get(profileField);
        }
    }
}
