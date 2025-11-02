# Memory Node

## Overview
Generic memory node for storing and retrieving values in workflow context. Similar to n8n's "Window Memory" or "Simple Memory" nodes.

## Key Features
- **Multiple Operations**: STORE, RETRIEVE, APPEND, CLEAR, LIST
- **Nested Execution Support**: Can be called from within other nodes (e.g., AI nodes)
- **Hierarchical Logging**: Shows proper parent->child execution hierarchy
- **Flexible Storage**: Supports any data type (strings, numbers, objects, lists)
- **Persistent by Design**: Outputs opt into `WriteOptions.persistent()` so values remain even without static consumers

## Operations

### STORE
Store a value in memory.
```json
{
  "operation": "STORE",
  "key": "conversation_history",
  "value": ["Hello", "How are you?"],
  "overwrite": true
}
```

### RETRIEVE
Retrieve a value from memory.
```json
{
  "operation": "RETRIEVE",
  "key": "conversation_history",
  "default_value": []
}
```

### APPEND
Append to a list (creates list if doesn't exist).
```json
{
  "operation": "APPEND",
  "key": "conversation_history",
  "value": "What's the weather?"
}
```

### CLEAR
Clear a value from memory.
```json
{
  "operation": "CLEAR",
  "key": "conversation_history"
}
```

### LIST
Get information about a key.
```json
{
  "operation": "LIST",
  "key": "conversation_history"
}
```

## Usage with AI Nodes

Memory nodes are perfect for AI conversation history:

```
GeminiNode -> Memory(STORE) -> Memory(APPEND) -> GeminiNode(with history)
```

Example workflow:
1. **Initialize**: `STORE` empty conversation history
2. **User Message**: `APPEND` user message to history
3. **AI Response**: `APPEND` AI response to history
4. **Next Turn**: `RETRIEVE` history for context

## Nested Execution

When AI nodes call memory nodes internally, logs show proper hierarchy:

```
[GeminiNode] Started
  [Memory:store] Started
  [Memory:store] Stored value at key: conversation_history
  [Memory:store] Finished
[GeminiNode] Retrieved conversation context
[GeminiNode] Finished

## Persistence & Safety

- **Context Policy Override**: The node definition is marked with `ContextAccessPolicy.PERSIST_OUTPUTS`, so the runtime skips the usual "must have a consumer" guard when flushing outputs.
- **Explicit Write Options**: STORE/APPEND operations call `context.write(..., WriteOptions.persistent())`, keeping values available for subsequent reads without forcing follow-up nodes to declare consumers.
- **Non-destructive Reads**: Persistent entries are read using `ExecutionContext.read` but the runtime leaves them intact, allowing multiple RETRIEVE calls inside the same workflow run.
- **Clear Removes Entry**: CLEAR now delegates to `context.remove`, ensuring persistent payloads are actually deleted instead of being replaced by `null` shadows.
```

## Architecture Changes

### NodeExecutionOrchestrator
New component that handles single node execution with proper logging context. Can be called:
- From `WorkflowEngineService` for workflow-level nodes
- From executors (like AI nodes) for nested/synthetic nodes

### Benefits
1. **Reusable execution logic**: No duplication between workflow engine and nested execution
2. **Hierarchical logging**: Proper parent->child context tracking
3. **Context flush control**: Memory operations complete before parent node finishes
4. **Flexible**: Supports both real nodes (in workflow) and synthetic nodes (internal operations)

## Use Cases

### 1. AI Conversation Memory
```json
{
  "nodes": {
    "init_memory": {
      "type": "memory",
      "config": {
        "input": {
          "operation": "STORE",
          "key": "chat_history",
          "value": []
        }
      }
    },
    "chat_loop": {
      "type": "gemini",
      "config": {
        "input": {
          "prompt": "${user_message}",
          "system_prompt": "Previous conversation: ${get('chat_history')}"
        }
      }
    },
    "save_response": {
      "type": "memory",
      "config": {
        "input": {
          "operation": "APPEND",
          "key": "chat_history",
          "value": "${chat_loop.output.ai_response}"
        }
      }
    }
  }
}
```

### 2. Data Aggregation
```json
{
  "operation": "APPEND",
  "key": "processed_results",
  "value": "${current_item.result}"
}
```

### 3. Temporary Storage
```json
{
  "operation": "STORE",
  "key": "temp_calculation",
  "value": "${calculate(a, b)}"
}
```

### 4. Cache Management
```json
{
  "operation": "RETRIEVE",
  "key": "api_cache:${endpoint}",
  "default_value": null
}
```

## Memory Scope
- **Workflow-scoped**: Memory persists within a single workflow execution
- **Context-backed**: Uses ExecutionContext for storage
- **Flush-controlled**: Memory writes flush when memory node succeeds
- **Cross-node**: Values accessible across all nodes in the workflow
