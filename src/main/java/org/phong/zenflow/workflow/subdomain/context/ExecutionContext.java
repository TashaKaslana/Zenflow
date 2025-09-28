package org.phong.zenflow.workflow.subdomain.context;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

@Builder
public class ExecutionContext {
    private final static String PROHIBITED_KEY_PREFIX = "__zenflow_";

    @Getter
    private final UUID workflowId;

    @Getter
    private final UUID workflowRunId;

    @Getter
    private final String traceId;

    @Getter
    private final UUID userId;

    @Getter
    private final NodeLogPublisher logPublisher;

    @Getter
    private String nodeKey;

    private final TemplateService templateService;
    private final RuntimeContextManager contextManager;

    /**
     * Reads a value from the runtime context with template resolution and type-safe casting.
     * <p>
     * This method resolves template expressions if applicable and ensures the returned value matches the specified type.
     *
     * @param key   the key to retrieve the value from the runtime context
     * @param clazz the expected class type of the value
     * @return the value associated with the key, or null if not found
     * @throws IllegalArgumentException if the key starts with a prohibited prefix
     * @throws ClassCastException       if the value cannot be cast to the specified type
     */
    public <T> T read(String key, Class<T> clazz) {
        // Allow internal reads for allowlisted reserved keys used by the engine
        boolean isReserved = key.startsWith(PROHIBITED_KEY_PREFIX);
        boolean isWhitelisted = ExecutionContextKey.CALLBACK_URL.matches(key);
        if (isReserved && !isWhitelisted) {
            throw new IllegalArgumentException("Access to reserved context keys is prohibited: " + key);
        }

        RuntimeContext context = getContext();
        if (context == null) return null;

        Object o;
        if (templateService != null && templateService.isTemplate(key)) {
            o = templateService.resolve(key, this);
        } else {
            o = context.getAndClean(nodeKey, key);
        }

        if (o == null) {
            return null;
        }
        if (clazz.isInstance(o)) {
            return clazz.cast(o);
        } else {
            throw new ClassCastException("Cannot cast context value to " + clazz.getName());
        }
    }

    private RuntimeContext getContext() {
        return contextManager.getOrCreate(workflowRunId.toString());
    }

    public void write(String key, Object value) {
        RuntimeContext context = getContext();
        if (context != null) {
            context.put(key, value);
        }
    }

    public void remove(String key) {
        RuntimeContext context = getContext();
        if (context != null) {
            context.remove(key);
        }
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
        if (logPublisher != null) {
            logPublisher.setNodeKey(nodeKey);
        }
    }

    /**
     * Returns the shared, immutable evaluator preconfigured with global
     * functions. Callers must {@link org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService.ImmutableEvaluator#cloneInstance() clone}
     * this instance before registering any custom functions to avoid
     * mutating the shared configuration.
     */
    public TemplateService.ImmutableEvaluator getEvaluator() {
        return templateService != null ? templateService.getEvaluator() : null;
    }

    public WorkflowConfig resolveConfig(String nodeKey, WorkflowConfig config) {
        if (config == null || config.input() == null) {
            return config;
        }
        String previous = this.nodeKey;
        this.nodeKey = nodeKey;
        Map<String, Object> resolvedInput = resolveMap(config.input());
        this.nodeKey = previous;

        return new WorkflowConfig(resolvedInput, config.profile(), config.output());
    }

    private Map<String, Object> resolveMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return map;
        }

        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> resolveValue(e.getValue())));
    }

    private Object resolveValue(Object value) {
        if (value instanceof String str) {
            if (templateService != null && templateService.isTemplate(str)) {
                return templateService.resolve(str, this);
            }
            return str;
        } else if (value instanceof Map<?, ?> m) {
            return resolveMap(ObjectConversion.convertObjectToMap(m));
        } else if (value instanceof List<?> list) {
            return list.stream().map(this::resolveValue).toList();
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCurrentNodeEntrypoint() {
        RuntimeContext context = getContext();
        return (Map<String, Object>) context.get(ExecutionContextKey.ENTRYPOINT_LIST_KEY + nodeKey);
    }

    @SuppressWarnings("unchecked")
    public Object getProfileSecret(String key) {
        RuntimeContext context = getContext();
        Map<String, Object> profiles = (Map<String, Object>) context.get(ExecutionContextKey.PROFILE_KEY.key());
        if (profiles == null) {
            return null;
        }

        Map<String, Object> nodeProfile = (Map<String, Object>) profiles.get(nodeKey);

        if (nodeProfile == null) {
            String nodeKey = this.nodeKey != null ? this.nodeKey : "unknown";
            throw new SecretDomainException("No profiles found in context for node: " + nodeKey);
        }

        Map<String, Object> secrets = (Map<String, Object>) nodeProfile.get("secrets");
        if (secrets != null) {
            return secrets.get(key);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public Object getSecret(String key) {
        RuntimeContext context = getContext();
        Map<String, Object> secretsList = (Map<String, Object>) context.get(ExecutionContextKey.SECRET_KEY.key());

        if (secretsList == null) {
            throw new SecretDomainException("No profiles found in context");
        }

        Map<String, Object> secrets = (Map<String, Object>) secretsList.get(nodeKey);
        if (secrets != null) {
            return secrets.get(key);
        }

        return null;
    }
}
