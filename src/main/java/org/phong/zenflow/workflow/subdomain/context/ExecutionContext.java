package org.phong.zenflow.workflow.subdomain.context;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.execution.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

@Getter
@Builder
public class ExecutionContext {
    private final UUID workflowId;
    private final UUID workflowRunId;
    private final String traceId;
    private final UUID userId;
    private final RuntimeContextManager contextManager;
    private final NodeLogPublisher logPublisher;
    private final TemplateService templateService;
    private String nodeKey;

    /**
     * Read a value from the runtime context with template resolution and consumer cleanup.
     * <p>
     * This should be preferred over {@link #get(String)} within executors as it resolves template
     * expressions and performs type-safe casting.
     */
    public <T> T read(String key, Class<T> clazz) {
        RuntimeContext context = contextManager.getOrCreate(workflowRunId.toString());
        if (context == null) {
            return null;
        }

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

    public void write(String key, Object value) {
        RuntimeContext context = contextManager.getOrCreate(workflowRunId.toString());
        if (context != null) {
            context.put(key, value);
        }
    }

    public void remove(String key) {
        RuntimeContext context = contextManager.getOrCreate(workflowRunId.toString());
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
     * functions. Callers must {@link org.phong.zenflow.workflow.subdomain.execution.services.TemplateService.ImmutableEvaluator#cloneInstance() clone}
     * this instance before registering any custom functions to avoid
     * mutating the shared configuration.
     */
    public TemplateService.ImmutableEvaluator getEvaluator() {
        return templateService != null ? templateService.getEvaluator() : null;
    }

    /**
     * Low-level access to the underlying runtime context for template evaluation.
     * <p>
     * Executors should use {@link #read(String, Class)} instead to ensure templates are resolved
     * and values are type-checked. This method still performs consumer tracking using the
     * currently set {@code nodeKey} and is primarily intended for internal usage by
     * {@link org.phong.zenflow.workflow.subdomain.execution.services.TemplateService}.
     */
    public Object get(String key) {
        RuntimeContext context = contextManager.getOrCreate(workflowRunId.toString());
        if (context == null) {
            return null;
        }
        return context.getAndClean(nodeKey, key);
    }

    public WorkflowConfig resolveConfig(String nodeKey, WorkflowConfig config) {
        if (config == null || config.input() == null) {
            return config;
        }
        String previous = this.nodeKey;
        this.nodeKey = nodeKey;
        Map<String, Object> resolvedInput = resolveMap(config.input());
        this.nodeKey = previous;
        return new WorkflowConfig(resolvedInput, config.output(), config.entrypoint());
    }

    private Map<String, Object> resolveMap(Map<String, Object> map) {
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
}