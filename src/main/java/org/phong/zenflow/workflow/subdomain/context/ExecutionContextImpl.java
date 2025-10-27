package org.phong.zenflow.workflow.subdomain.context;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Setter;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.workflow.subdomain.context.refvalue.ExecutionOutputEntry;
import org.phong.zenflow.workflow.subdomain.context.refvalue.WriteOptions;
import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

@Builder
public class ExecutionContextImpl implements ExecutionContext {

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

    @Getter
    @Setter
    private UUID pluginNodeId;

    @Setter
    private ScopedNodeResource<?> scopedResource;

    private final TemplateService templateService;
    private final RuntimeContextManager contextManager;
    private final ContextValueResolver contextValueResolver;

    @Builder.Default
    private Map<String, WorkflowConfig> nodeConfigs = new ConcurrentHashMap<>();

    @Getter
    private WorkflowConfig currentConfig;

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
        return read(key, clazz, ReadOptions.DEFAULT);
    }
    
    /**
     * Reads a value from the runtime context with configurable priority.
     * 
     * @param key the key to retrieve
     * @param clazz the expected class type
     * @param options read options controlling config vs context priority
     * @return the value associated with the key, or null if not found
     */
    public <T> T read(String key, Class<T> clazz, ReadOptions options) {
        // Allow internal reads for allowlisted reserved keys used by the engine
        boolean isReserved = key.startsWith(ExecutionContextKey.PROHIBITED_KEY_PREFIX.key());
        boolean isWhitelisted = ExecutionContextKey.CALLBACK_URL.matches(key);
        if (isReserved && !isWhitelisted) {
            throw new IllegalArgumentException("Access to reserved context keys is prohibited: " + key);
        }

        RuntimeContext context = getContext();

        Object value;
        value = contextValueResolver.resolve(
                workflowRunId,
                nodeKey,
                key,
                currentConfig,
                context,
                templateService,
                this,
                options
        );

        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        throw new ClassCastException("Cannot cast context value to " + clazz.getName());
    }

    private RuntimeContext getContext() {
        return contextManager.getOrCreate(workflowRunId.toString());
    }

    public void write(String key, Object value, WriteOptions options) {
        RuntimeContext context = getContext();
        if (context != null) {
            context.write(key, value, options);
        }
    }

    public void write(String key, Object value) {
        write(key, value, WriteOptions.DEFAULT);
    }

    public void remove(String key) {
        RuntimeContext context = getContext();
        if (context != null) {
            context.remove(key);
        }
    }

    @Override
    public String taskId() {
            return workflowRunId + "-" + nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
        if (logPublisher != null) {
            logPublisher.setNodeKey(nodeKey);
        }
        if (nodeConfigs != null) {
            this.currentConfig = nodeConfigs.get(nodeKey);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getResource() {
        if (scopedResource == null) {
            throw new IllegalStateException("No resource is associated with this execution context");
        }

        return (T) scopedResource.getResource();
    }

    public <T> T getResource(Class<T> type) {
        if (scopedResource == null) {
            throw new IllegalStateException("No resource is associated with this execution context");
        }
        Object resource = scopedResource.getResource();

        if (!type.isInstance(resource)) {
            throw new ClassCastException(
                    "Expected resource of type " + type.getName() +
                            " but got " + resource.getClass().getName()
            );
        }

        return type.cast(resource);
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

    public void setCurrentConfig(WorkflowConfig config) {
        this.currentConfig = config;
        if (nodeKey != null && nodeConfigs != null) {
            if (config != null) {
                nodeConfigs.put(nodeKey, config);
            } else {
                nodeConfigs.remove(nodeKey);
            }
        }
    }


    @SuppressWarnings("unchecked")
    public Map<String, Object> getCurrentNodeEntrypoint() {
        RuntimeContext context = getContext();
        Map<String, Object> entrypoint = context != null
                ? (Map<String, Object>) context.get(ExecutionContextKey.ENTRYPOINT_LIST_KEY + nodeKey)
                : null;
        if (entrypoint != null) {
            return entrypoint;
        }
        if (currentConfig != null && currentConfig.input() != null) {
            return new HashMap<>(currentConfig.input());
        }
        return Map.of();
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

    @Override
    public boolean containsKey(String key) {
        RuntimeContext context = getContext();
        if (contextValueResolver.hasConfigValue(nodeKey, key, currentConfig)) {
            return true;
        }
        return context != null && context.containsKey(key);
    }

    @Override
    public void writeAll(Map<String, Object> values, WriteOptions options) {
        RuntimeContext context = getContext();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            context.write(entry.getKey(), entry.getValue(), options);
        }
    }

    @Override
    public void writeAllEntries(Map<String, ExecutionOutputEntry> entries) {
        RuntimeContext context = getContext();
        for (Map.Entry<String, ExecutionOutputEntry> entry : entries.entrySet()) {
            ExecutionOutputEntry outputEntry = entry.getValue();
            context.write(outputEntry.key(), outputEntry.value(), outputEntry.writeOptions());
        }
    }

    @Override
    public <T> T readOrDefault(String key, Class<T> clazz, T defaultValue) {
        return readOrDefault(key, clazz, defaultValue, ReadOptions.DEFAULT);
    }
    
    @Override
    public <T> T readOrDefault(String key, Class<T> clazz, T defaultValue, ReadOptions options) {
        T value = read(key, clazz, options);
        return value != null ? value : defaultValue;
    }
}
