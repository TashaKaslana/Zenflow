package org.phong.zenflow.workflow.subdomain.context;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.secret.subdomain.aggregate.AggregatedSecretDto;
import org.phong.zenflow.secret.subdomain.link.service.SecretLinkService;
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
    private final SecretLinkService secretLinkService;

    /**
     * Resolves workflow config by handling zenflow.secrets.* and zenflow.profiles.* templates
     * during the definition phase.
     */
    public ResolvedResult resolveConfig(WorkflowConfig config, UUID workflowId, String nodeKey) {
        if (config == null || config.input() == null) {
            return new ResolvedResult(
                    config,
                    null
            );
        }

        Map<String, Object> resolvedInput = resolveMap(config.input(), workflowId, nodeKey);
        Map<String, String> profileForWorkflowNode = secretLinkService.getProfileForWorkflowNode(workflowId, nodeKey);
        return new ResolvedResult(
                new WorkflowConfig(resolvedInput, config.profile(), config.output()),
                profileForWorkflowNode
        );
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
            AggregatedSecretDto agg = new AggregatedSecretDto(secretLinkService.getSecretsForWorkflowNode(workflowId, nodeKey));

            // Create a definition-phase resolver
            DefinitionPhaseResolver resolver = new DefinitionPhaseResolver(agg);

            return templateService.resolveDefinitionPhase(template, workflowId, resolver);
        } catch (Exception e) {
            // If resolution fails, return the original template
            return template;
        }
    }

    /**
     * Internal class that provides definition-phase resolution for secrets and profiles
     */
    private record DefinitionPhaseResolver(
            AggregatedSecretDto aggregatedData) implements TemplateService.ReservedValueResolver {
        /**
         * Resolves secret value by key for the current workflow
         */
        @Override
        public String resolveSecretValue(UUID workflowId, String secretKey) {
            return aggregatedData.secrets().get(secretKey);
        }
    }

    public record ResolvedResult(WorkflowConfig config, Map<String, String> profiles) {
    }
}
