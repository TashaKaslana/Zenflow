# AI Cluster & Abstract Layer TODO

This note captures the current state of the AI integration layer and lists the work required to establish a clear split between **cluster** (composition/orchestration) nodes and **abstract** (provider-specific) nodes.

---

## 🎯 **Implementation Status**

### ✅ Phase 1: Foundation & DTOs (COMPLETED)
- [x] Created `AiExecutionRequest` - Typed request with messages, tools, options
- [x] Created `AiExecutionResult` - Structured result with output, metadata, parse status
- [x] Created `AiModelCapabilities` - Provider capability metadata
- [x] Created `AiClusterConfig` - User-facing cluster configuration DTO
- [x] Created `AiExecutionRequestFactory` - Factory for building typed requests
- [x] Updated `AiModelProvider` interface - Now uses `execute(AiExecutionRequest)` instead of raw `call()`
- [x] Refactored `GeminiModelProvider` - Accepts typed requests, returns structured results
- [x] Updated `AiExecutor` - Simplified to adapter pattern, delegates to provider
- [x] Verified compilation - All changes compile successfully

**Next:** Phase 2 - Create AI Cluster Node

---

## Current Observations
- `src/main/java/org/phong/zenflow/plugin/subdomain/nodes/builtin/integration/ai/base/AiExecutor.java:30` mixes orchestration concerns (prompt assembly, memory, response formatting) with provider invocation, making it hard to reuse across cluster variants.
- The `AiToolRegistry` is copied in `GeminiAiExecutor` but is not wired into the provider call path; `GeminiModelProvider` ignores the registry entirely (`src/main/java/.../gemini/GeminiModelProvider.java:18`), so tool support is effectively absent.
- `AiExecutor` defaults to destructive writes via `context.write(...)` which still rely on pending-write flush (okay after memory changes) but there is no hook for conversation history or tool observations.
- Model-specific options are passed as a loose `Map` without validation, causing runtime failures when types are wrong (`src/main/java/.../AiExecutor.java:53`).
- There is no cluster node or schema that can orchestrate multiple AI nodes, memory nodes, or tool wiring; the existing Gemini node is effectively both cluster + abstract.
- Observability scaffolding (`AiObservationRegistry`) is copied but never populated with handlers beyond the default (`GeminiModelProvider` ignores it).
- The resource manager returns a bare `VertexAiGeminiChatModel`, but the cluster/abstract split will require shared context (e.g., tool bindings, memory) that the current resource lifecycle does not expose.

---

## Target Architecture
1. **Cluster Node (`core:ai.cluster` placeholder)**
   - Coordinates prompts, memories, tool registries, retry policy.
   - Delegates to abstract model nodes via the orchestrator (`NodeExecutionOrchestrator`).
   - Handles conversation state (reads / writes to memory node using new persistent context behavior).
   - Applies platform policy (rate limits, budget, fallback ordering).

2. **Abstract Nodes (e.g., `google-ai:gemini`, future `openai:chat`)**
   - Provide model-specific execution (`AiModelProvider` implementations).
   - Expose capabilities metadata (tool support, streaming, JSON schema validation).
   - Rely on the cluster to supply prompt/messages, tool handles, and persistence flags.

3. **Shared Contracts**
   - Clearly typed request/response DTOs instead of raw `Map` configuration.
   - Tool/observation registry injection surface that both cluster and abstract nodes respect.
   - Policy hooks so platform managers can enforce rate limits/timeouts without editing providers.

---

## Cluster Composition Model

The cluster behaves like a composite node whose children represent distinct responsibilities:

| Sub-node | Responsibility | Notes |
| --- | --- | --- |
| **Tools** | Provides default tool pack (platform maintained) and optional custom tool nodes. | Custom tools register against the cluster-local registry before model execution. |
| **Model** | Required abstract node that wraps a Spring AI `ChatModel` provider. | Supports selection via schema enum; cluster wires the chosen model into execution. |
| **Context** | Handles conversation state; defaults to memory node backed by persistent context writes. | Alternate context nodes (vector store, database) plug in later. |
| **Output Parser** | Normalizes raw model output (text → JSON, JSON → typed DTO). | Pluggable so workflows can swap parsers without touching model or cluster. |

Execution flow:
1. Cluster gathers inputs (prompt, context references) and resolves child sub-nodes.
2. Tools sub-node registers default + workflow-defined tools onto the `AiToolRegistry`.
3. Context sub-node fetches conversation state into the `AiExecutionRequest`.
4. Model sub-node executes using the constructed request and capability metadata.
5. Output parser sub-node transforms the model response and emits final payload.
6. Cluster writes back memory/context updates and exposes aggregated metadata.

This structure keeps the root cluster node focused on orchestration while allowing each concern to evolve independently and be reused by other orchestrators.

---

## Configuration Surface

Although the internal implementation treats tools, model, context, and parser as sub-components, the workflow author should only configure a single cluster node. The node schema can expose friendly selectors with sensible defaults, for example:

```yaml
type: core:ai.cluster
config:
  tool: default            # optional, defaults to platform tool pack
  model: openai:gpt-5      # required; maps to registered abstract node/provider
  memory: default          # optional, maps to core:context_variable
  output_parser: json      # optional, selects parser strategy
  credentials:
    api_key: "${secrets.OPENAI_API_KEY}"
  model_options:
    temperature: 0.6
    max_tokens: 1024
```

The cluster executor translates this schema into the appropriate `BaseWorkflowNode` instances under the hood, so UI users just drag one node, choose a model, and optionally override defaults. This keeps the orchestration flexible while maintaining a simple configuration experience.

---

## Sample Interfaces & Stub

```java
public interface AiExecutionRequestFactory {
    AiExecutionRequest build(AiClusterConfig config,
                             AiToolRegistry toolRegistry,
                             ConversationContext conversation);
}

public interface AiExecutionRequest {
    List<Message> messages();
    Map<String, Object> modelOptions();
    ContextSnapshot context();
}

public interface AiExecutionResult {
    Object output();
    String rawResponse();
    Map<String, Object> metadata();
}

@Component
@PluginNode(key = "core:ai.cluster", name = "AI Cluster", version = "1.0.0")
public class AiClusterNode implements NodeDefinitionProvider {
    private final ToolNode toolNode;
    private final ModelNode modelNode;
    private final ContextNode contextNode;
    private final OutputParserNode parserNode;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(new AiClusterExecutor(toolNode, modelNode, contextNode, parserNode))
                .build();
    }
}
```

These snippets illustrate how the cluster orchestrator composes its sub-nodes and shares typed request/response objects with provider-specific executors.

---

## TODO Checklist
### Cluster Layer
- [ ] Define cluster node schema: prompts, memories, tool wiring, fallback chain, retry policy.
- [ ] Implement cluster executor that builds `AiExecutionRequest` (messages + tool context) and calls abstract node via `NodeExecutionOrchestrator`.
- [ ] Add conversation memory bridge leveraging `WriteOptions.persistent()` to keep transcripts alive.
- [ ] Support multi-model dispatch (primary + fallback) with structured error handling.
- [ ] Surface execution metadata (latency, cost, provider) back to cluster outputs.
- [ ] Define child node contracts for tools/model/context/output parser and register default implementations.

### Abstract Layer
- [ ] Refine `AiExecutor` into a pure abstract executor that accepts a typed request instead of reading config from context.
- [ ] Update `GeminiModelProvider` to consume tool registry & observation registry; apply Vertex AI specific capabilities (function calling, JSON schema).
- [ ] Introduce provider capability flags (supports tools, supports json-mode, streaming, etc.) for cluster negotiation.
- [ ] Harden option handling with validated config classes (e.g., record + Jackson binding) instead of `Map`.
- [ ] Add tests per provider covering tool/no-tool, text/json, and failure scenarios.

### Shared Infrastructure
- [ ] Create `AiExecutionRequest`/`AiExecutionResult` DTOs shared by cluster & abstract nodes.
- [ ] Extend `AiToolRegistry` with namespacing + metadata so cluster can advertise available tools to providers.
- [ ] Wire `AiObservationRegistry` to record per-call metrics and propagate to Micrometer/Tracing.
- [ ] Document the contract in `/docs/ai-cluster-guide.md` with examples (cluster calling Gemini, cluster with multi-model fallback, cluster + memory).
- [ ] Provide parser and context base interfaces so future clusters can reuse the same sub-node ecosystem.

---

## Open Questions
1. How will third-party plugins register additional abstract model nodes without access to internal cluster types?  
   **Answer:** Follow the current Gemini pattern—providers expose tools internally and register as abstract nodes. Cluster will call them via the orchestrator so third parties can keep shipping self-contained providers.
2. Should tool execution occur inside the cluster (pre/post model call) or be delegated to provider-specific tool calling (e.g., Gemini vs OpenAI function calling)?  
   **Answer:** Tools remain a default cluster capability; providers may ignore them, but cluster always supplies the standard tool set.
3. What failure policy should the cluster adopt when provider throttling occurs - retry locally, fall back, or bubble up?  
   **Answer:** Treat abstract providers like any other node—let resilience policies decide (retry/fallback) and surface errors through the orchestration pipeline.
4. How can we version the cluster schema to allow incremental feature rollout without breaking existing workflows?  
   **Answer:** Still in development; no production consumers yet, so we can iterate without compatibility guarantees.
