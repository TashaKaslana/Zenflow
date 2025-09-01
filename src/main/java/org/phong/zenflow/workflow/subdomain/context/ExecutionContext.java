package org.phong.zenflow.workflow.subdomain.context;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;

@Getter
@Builder
public class ExecutionContext {
    private final UUID workflowId;
    private final UUID workflowRunId;
    private final String traceId;
    private final UUID userId;
    private final RuntimeContextManager contextManager;
    private final NodeLogPublisher logPublisher;
    private String nodeKey;

    public <T> T read(String key, Class<T> clazz) {
        RuntimeContext context = contextManager.getOrCreate(workflowRunId.toString());
        if (context == null) {
            return null;
        }

        Object o = context.get(key);

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
}