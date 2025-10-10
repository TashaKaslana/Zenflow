package org.phong.zenflow.workflow.subdomain.context;

import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.util.Map;
import java.util.UUID;

public interface ExecutionContext {
    String taskId();

    UUID getWorkflowRunId();

    UUID getWorkflowId();

    String getTraceId();

    UUID getUserId();

    String getNodeKey();

    void setNodeKey(String nodeKey);

    NodeLogPublisher getLogPublisher();

    UUID getPluginNodeId();

    void setPluginNodeId(UUID pluginNodeId);

    void setScopedResource(ScopedNodeResource<?> resource);

    <T> T read(String key, Class<T> clazz);

    WorkflowConfig getCurrentConfig();

    void setCurrentConfig(WorkflowConfig config);

    void write(String key, Object value);

    void remove(String key);

    <T> T getResource();

    <T> T getResource(Class<T> type);

    TemplateService.ImmutableEvaluator getEvaluator();

    WorkflowConfig resolveConfig(String nodeKey, WorkflowConfig config);

    Map<String, Object> getCurrentNodeEntrypoint();

    Object getProfileSecret(String key);

    Object getSecret(String key);
}
