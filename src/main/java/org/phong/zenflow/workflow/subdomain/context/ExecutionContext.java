package org.phong.zenflow.workflow.subdomain.context;

import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import org.phong.zenflow.workflow.subdomain.context.refvalue.ExecutionOutputEntry;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.WriteOptions;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

import java.io.IOException;
import java.io.InputStream;
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
    
    <T> T read(String key, Class<T> clazz, ReadOptions options);

    <T> T readOrDefault(String key, Class<T> clazz, T defaultValue);
    
    <T> T readOrDefault(String key, Class<T> clazz, T defaultValue, ReadOptions options);

    boolean containsKey(String key);
    
    /**
     * Opens a stream to read the value as raw bytes without full materialization.
     * Useful for large binary payloads (files, videos, images) that should be streamed
     * rather than loaded entirely into memory.
     * 
     * <p>The caller is responsible for closing the returned InputStream.
     * 
     * <p>Example usage:
     * <pre>
     * try (InputStream stream = context.openStream("payload.video")) {
     *     // Stream to external service or write to file
     *     uploadService.upload(stream);
     * }
     * </pre>
     * 
     * @param key the context key to stream
     * @return InputStream over the value's raw bytes
     * @throws IOException if the stream cannot be opened or the value doesn't exist
     */
    InputStream openStream(String key) throws IOException;

    WorkflowConfig getCurrentConfig();

    void setCurrentConfig(WorkflowConfig config);

    void write(String key, Object value, WriteOptions options);

    default void write(String key, Object value) {
        write(key, value, WriteOptions.DEFAULT);
    }
    
    /**
     * Writes a value from an InputStream for efficient streaming of large binary data.
     * The stream will be consumed and stored according to the specified WriteOptions.
     * The caller should NOT close the stream - it will be closed by this method.
     * 
     * <p>This is more efficient than reading the entire stream into memory first
     * for large files, videos, or other binary payloads.
     * 
     * <p>Example usage:
     * <pre>
     * try (InputStream fileStream = Files.newInputStream(path)) {
     *     WriteOptions options = new WriteOptions("video/mp4", StoragePreference.FILE, true);
     *     context.writeStream("payload.video", fileStream, options);
     * }
     * </pre>
     * 
     * @param key the context key to write to
     * @param inputStream the stream to read data from (will be closed by this method)
     * @param options write options controlling storage behavior
     * @throws IOException if the stream cannot be read or written
     */
    void writeStream(String key, InputStream inputStream, WriteOptions options) throws IOException;
    
    default void writeStream(String key, InputStream inputStream) throws IOException {
        writeStream(key, inputStream, WriteOptions.DEFAULT);
    }

    void writeAll(Map<String, Object> values, WriteOptions options);

    default void writeAll(Map<String, Object> values) {
        writeAll(values, WriteOptions.DEFAULT);
    }

    void writeAllEntries(Map<String, ExecutionOutputEntry> entries);

    void remove(String key);

    <T> T getResource();

    <T> T getResource(Class<T> type);

    TemplateService.ImmutableEvaluator getEvaluator();

    Map<String, Object> getCurrentNodeEntrypoint();

    Object getProfileSecret(String key);

    Object getSecret(String key);
}
