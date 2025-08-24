package org.phong.zenflow.workflow.subdomain.context;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;

import java.util.UUID;

@Getter
@Builder
public class ExecutionContext {
    private final UUID workflowId;
    private final UUID workflowRunId;
    private final String traceId;
    @Setter
    private String nodeKey;
    private final UUID userId;
    private final NodeLogPublisher log;

    private final RuntimeContextManager contextManager;

    public <T> T read(String key, Class<T> clazz) {
        Object o = contextManager
                .getOrCreate(workflowRunId.toString())
                .get(key);

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
        contextManager
                .getOrCreate(workflowRunId.toString())
                .put(key, value);
    }

    public void remove(String key) {
        contextManager
                .getOrCreate(workflowRunId.toString())
                .remove(key);
    }
}