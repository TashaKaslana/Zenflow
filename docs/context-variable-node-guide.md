# Context Variable Node - Usage Guide

## Overview
The **Context Variable** node (`core:context_variable`) provides a way to store and retrieve variables in workflow context. It's specifically designed for:
- **AI conversation history**: Store chat messages across turns
- **Temporary data**: Cache API responses or intermediate calculations
- **Data aggregation**: Build lists across loop iterations

## Key Features
- ✅ **Multiple Operations**: STORE, RETRIEVE, APPEND, CLEAR, LIST
- ✅ **Nested Execution**: Can be called from within other executors (AI nodes)
- ✅ **Hierarchical Logging**: Shows proper parent→child execution in logs
- ✅ **Type Flexible**: Works with strings, numbers, objects, arrays

## Using from AI Nodes

AI executors can call context variable operations internally:

```java
// In GeminiAiExecutor or similar
public ExecutionResult execute(ExecutionContext context) {
    // Retrieve conversation history
    WorkflowConfig retrieveConfig = new WorkflowConfig();
    retrieveConfig.setInput(Map.of(
        "operation", "RETRIEVE",
        "key", "conversation_history",
        "default_value", List.of()
    ));
    
    ExecutionResult historyResult = nodeExecutionOrchestrator
        .executeSyntheticNodeByKey(
            "core:context_variable:1.0.0",
            "retrieve_history",
            retrieveConfig,
            context
        );
    
    // Use history in AI prompt...
    
    // Store new message
    WorkflowConfig appendConfig = new WorkflowConfig();
    appendConfig.setInput(Map.of(
        "operation", "APPEND",
        "key", "conversation_history",
        "value", Map.of(
            "role", "assistant",
            "content", aiResponse
        )
    ));
    
    nodeExecutionOrchestrator.executeSyntheticNodeByKey(
        "core:context_variable:1.0.0",
        "save_message",
        appendConfig,
        context
    );
    
    return ExecutionResult.success();
}
```

## Logs Will Show

```
[GeminiNode] Started
  [retrieve_history] Synthetic node started
  [retrieve_history] Variable operation: RETRIEVE on key: conversation_history
  [retrieve_history] Synthetic node finished with status: SUCCESS
[GeminiNode] Processing with 5 messages in history
  [save_message] Synthetic node started
  [save_message] Variable operation: APPEND on key: conversation_history
  [save_message] Synthetic node finished with status: SUCCESS
[GeminiNode] Finished
```

## Architecture Decisions

### Why "Context Variable" instead of "Memory"?
- **Specific Purpose**: "Memory" is too generic - could mean memory management, RAM usage, etc.
- **Clear Intent**: "Context Variable" clearly indicates workflow-scoped temporary storage
- **Disambiguation**: Avoids confusion with other memory-related features

### Why Composite Key Lookup?
- **Problem**: Registry uses UUID as key, but executors need to call nodes by name
- **Solution**: Added `compositeKeyToIdMap` in `PluginNodeExecutorRegistry`
- **Benefit**: AI nodes can call `core:context_variable:1.0.0` without knowing the UUID

### Why No Try-Catch in Executor?
- **Default Handling**: Pipeline automatically treats exceptions as NON_RETRIABLE
- **Cleaner Code**: No unnecessary wrapping
- **Consistent**: Matches other executors in the system

## Example: AI Chat with History

```json
{
  "nodes": {
    "init_chat": {
      "type": "plugin",
      "pluginNode": {
        "pluginKey": "core",
        "nodeKey": "context_variable"
      },
      "config": {
        "input": {
          "operation": "STORE",
          "key": "chat_history",
          "value": []
        }
      }
    },
    "user_message": {
      "type": "plugin",
      "pluginNode": {
        "pluginKey": "core",
        "nodeKey": "context_variable"
      },
      "config": {
        "input": {
          "operation": "APPEND",
          "key": "chat_history",
          "value": {
            "role": "user",
            "content": "${user_input}"
          }
        }
      }
    },
    "ai_chat": {
      "type": "plugin",
      "pluginNode": {
        "pluginKey": "google.ai",
        "nodeKey": "gemini"
      },
      "config": {
        "input": {
          "prompt": "${user_input}",
          "system_prompt": "Previous conversation: ${get('chat_history')}"
        }
      }
    }
  }
}
```

## Implementation Details

### Registry Updates
- **`PluginNodeExecutorRegistry`**: Added `compositeKeyToIdMap` and `registerCompositeKey()`
- **`PluginNodeSynchronizer`**: Calls `registerCompositeKey()` during node registration

### Orchestrator Updates
- **`NodeExecutionOrchestrator`**: New method `executeSyntheticNodeByKey()` for composite key lookup
- **Workflow Engine**: Delegates to orchestrator for reusable execution logic

### Benefits
1. **Reusable execution**: No code duplication
2. **Hierarchical logs**: Proper parent→child context
3. **Context flush control**: Variables persist when node succeeds
4. **Flexible**: Works from workflows or executors
