package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.memory;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.WriteOptions;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generic context variable node for storing/retrieving context values.
 * Supports multiple operations: STORE, RETRIEVE, APPEND, CLEAR, LIST
 * 
 * Enables AI nodes to persist conversation history and intermediate results.
 * Works like n8n's window memory or simple memory nodes.
 */
@Component
@Slf4j
public class MemoryExecutor implements NodeExecutor {
    
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Starting context variable operation");
        
        String operation = context.read("operation", String.class);
        String key = context.read("key", String.class);
        
        if (key == null || key.isBlank()) {
            return ExecutionResult.error(ExecutionError.NON_RETRIABLE, "Variable key is required");
        }
        
        MemoryOperation op = MemoryOperation.fromString(operation);
        logs.info("Variable operation: {} on key: {}", op, key);
        
        Object result = switch (op) {
            case STORE -> handleStore(context, key, logs);
            case RETRIEVE -> handleRetrieve(context, key, logs);
            case APPEND -> handleAppend(context, key, logs);
            case CLEAR -> handleClear(context, key, logs);
            case LIST -> handleList(context, key, logs);
        };
        
        context.write("result", result);
        logs.info("Variable operation completed successfully");
        
        return ExecutionResult.success();
    }
    
    private Object handleStore(ExecutionContext context, String key, NodeLogPublisher logs) {
        Object value = context.read("value", Object.class);
        Boolean overwrite = context.readOrDefault("overwrite", Boolean.class, true);
        
        // Check if key exists
        Object existing = context.read(key, Object.class);
        if (existing != null && !overwrite) {
            logs.warn("Key already exists and overwrite=false: {}", key);
            return Map.of("stored", false, "reason", "key_exists");
        }
        
        context.write(key, value, WriteOptions.persistent());
        logs.info("Stored value at key: {}", key);
        
        return Map.of(
            "stored", true,
            "key", key,
            "size", calculateSize(value)
        );
    }
    
    private Object handleRetrieve(ExecutionContext context, String key, NodeLogPublisher logs) {
        Object value = context.read(key, Object.class);
        
        if (value == null) {
            Object defaultValue = context.readOrDefault("default_value", Object.class, null);
            logs.info("Key not found, using default: {}", key);
            return Map.of(
                "found", false,
                "key", key,
                "value", defaultValue
            );
        }
        
        logs.info("Retrieved value from key: {}", key);
        return Map.of(
            "found", true,
            "key", key,
            "value", value
        );
    }
    
    private Object handleAppend(ExecutionContext context, String key, NodeLogPublisher logs) {
        Object newValue = context.read("value", Object.class);
        Object existing = context.read(key, Object.class);
        
        List<Object> list;
        if (existing == null) {
            list = new ArrayList<>();
        } else if (existing instanceof List) {
            list = new ArrayList<>((List<?>) existing);
        } else {
            // Convert existing single value to list
            list = new ArrayList<>();
            list.add(existing);
        }
        
        list.add(newValue);
        context.write(key, list, WriteOptions.persistent());
        
        logs.info("Appended to key: {}, new size: {}", key, list.size());
        return Map.of(
            "appended", true,
            "key", key,
            "size", list.size()
        );
    }
    
    private Object handleClear(ExecutionContext context, String key, NodeLogPublisher logs) {
        Object existing = context.read(key, Object.class);
        boolean existed = existing != null;
        
        if (existed) {
            context.remove(key);
            logs.info("Cleared key: {}", key);
        } else {
            logs.info("Key does not exist: {}", key);
        }
        
        return Map.of(
            "cleared", existed,
            "key", key
        );
    }
    
    private Object handleList(ExecutionContext context, String key, NodeLogPublisher logs) {
        // List all keys matching a pattern (if key contains '*')
        // For now, just return the specific key info
        Object value = context.read(key, Object.class);
        
        Map<String, Object> info = new HashMap<>();
        info.put("key", key);
        info.put("exists", value != null);
        if (value != null) {
            info.put("type", value.getClass().getSimpleName());
            info.put("size", calculateSize(value));
        }
        
        logs.info("Listed key info: {}", key);
        return info;
    }
    
    private int calculateSize(Object value) {
        if (value == null) return 0;
        if (value instanceof List) return ((List<?>) value).size();
        if (value instanceof Map) return ((Map<?, ?>) value).size();
        if (value instanceof String) return ((String) value).length();
        return 1;
    }
    
    enum MemoryOperation {
        STORE,      // Store a value
        RETRIEVE,   // Retrieve a value
        APPEND,     // Append to a list
        CLEAR,      // Clear a value
        LIST;       // List key info
        
        static MemoryOperation fromString(String op) {
            if (op == null) return STORE;
            return switch (op.toUpperCase()) {
                case "STORE", "SET", "WRITE" -> STORE;
                case "RETRIEVE", "GET", "READ" -> RETRIEVE;
                case "APPEND", "ADD", "PUSH" -> APPEND;
                case "CLEAR", "DELETE", "REMOVE" -> CLEAR;
                case "LIST", "INFO", "DESCRIBE" -> LIST;
                default -> STORE;
            };
        }
    }
}
