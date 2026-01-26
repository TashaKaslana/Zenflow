# AI Cluster Design & TODOs

This document captures the current behaviour of the AI Cluster node (`core:ai.cluster`) and the gaps to close to reach a flexible, n8n-style cluster that can route tools, memory, parsers, and providers.

---

## Current State (what ships today)
- **Provider orchestration**: `AiClusterExecutor` resolves a provider via `AiClusterProviderResolver` and invokes the child provider node through `NodeExecutionOrchestrator`.
- **Request build**: `AiExecutionRequestFactory` builds messages (system + user + optional history), copies global `AiToolRegistry` tools, and model options.
- **Tools**: Only the default global `AiToolRegistry` is used. The tooling child node (`core:ai.cluster.tools`) is materialized but ignored at runtime.
- **Memory**: History load/append is hard‑wired to the synthetic `core:context_variable:1.0.0` node. No backend selection or memory-as-tool contract.
- **Parser**: No real parser wiring. `response_format=json` only triggers a Map/List check. The parser child node is configuration-only.
- **Schema**: `cluster/schema.json` exposes prompt/system/model/options/history, child overrides, and has a provider enum typo (`opernrouter:chat`). Tools/memory/parser fields are missing.
- **Tests**: None for cluster tooling, memory lifecycle, parser behaviour, or provider routing.

---

## Target Architecture
What the cluster should provide to be “basic but flexible”:

1) **ToolRouter**  
   - Interface + default implementation that reads a tooling config and produces Spring AI tool objects.  
   - Supports:
     - Function-call tools (e.g., @Tool beans by name / pack).
     - Node-as-tool adapters (invoke a workflow node as a tool).
     - Memory-as-tool (memory operations exposed as a tool).  
   - Optional enable/disable, default packs, merge with platform defaults.

2) **Memory as a Tool / Swappable Backend**  
   - Abstract memory backend (in-memory, KV/context node, vector, external).  
   - Contract: `load(session/ns, max_history)`, `append(turn)`, optional `flush/commit`.  
   - Pluggable via `child_nodes.context` override or a `memory` section in cluster input.

3) **Parser Strategy**  
   - Interface + strategies: `auto`, `text`, `json` (+ validation), `schema/structured`, `raw`.  
   - Applies after provider call; controls parse success flag and error handling (`fail` vs. `fallback`), retries if needed.

4) **Child Wiring**  
   - Use compound child overrides (`child_nodes`, `child_executor_types`) to choose tools/context/parser nodes.  
   - At runtime, `AiClusterExecutor` reads child configs, builds per-run ToolRouter, memory backend, and parser, then passes the tool list into `AiExecutionRequestFactory`.

5) **Provider Call**  
   - Keep provider resolution via `AiClusterProviderResolver`; pass prompt/system/model_options/tools/response_format.  
   - Stash typed request via `AiExecutionContextKeys.TYPED_REQUEST` before invoking provider.

---

## Schema Updates (to implement)
- **cluster/schema.json**
  - Fix typo: provider enum `openrouter:chat` (remove `opernrouter:chat`).
  - Add `tools` object:
    - `router` (string, default `default`)
    - `default_pack` (enum platform/minimal/extended)
    - `custom_tools` (array of bean names)
    - `node_tools[]` (plugin ref `<pluginKey>:<nodeKey>:<version>`, input schema/object)
    - `memory_tool` (boolean/object) to enable memory-as-tool injection
    - `enabled` (boolean)
  - Add `memory` object:
    - `backend` enum (`in_memory`, `context`, `kv`, `vector`, `external`)
    - `key`, `namespace`, `session_id`, `max_history_messages`, `ttl`, `persistent`
    - `backend_node` override (plugin ref) when using node-backed memory
  - Add `parser` object:
    - `strategy` enum (`auto`, `text`, `json`, `schema`, `raw`)
    - `schema`/`expected_shape`, `on_error` (`fail`, `fallback_to_text`), `max_retries`
- **tools.schema.json**
  - Reflect routing options above instead of only `default_pack`/`custom_tools`.
- **output-parser.schema.json**
  - Add strategy-specific options (`schema`, `coerce`, `on_error`, `max_retries`).

---

## Execution Flow (target)
1) Build `AiClusterConfig` from node input (prompt/system/model/options/history/tool/memory/parser).
2) Resolve provider child via `AiClusterProviderResolver` or existing child.
3) Build tool list via ToolRouter (function tools + node-as-tool + memory-as-tool).
4) Load history from selected memory backend; cap to `max_history_messages`; attach to messages.
5) Build `AiExecutionRequest` (messages, modelOptions, toolObjects, responseFormat, contextMetadata).
6) Call provider child through orchestrator; read response/raw/metadata.
7) Apply parser strategy to produce `output` + `parse_success`; capture parse errors.
8) Append turn to memory backend (respect persistence/ttl).
9) Write outputs/metadata to context.

---

## Action Items for this branch
1) **Tooling**: Introduce `ToolRouter` interface + default impl; wire into `AiClusterExecutor` and `AiExecutionRequestFactory`; read tooling child config.  
2) **Memory**: Add memory backend abstraction; allow selection via cluster input/child override; stop hardcoding `core:context_variable`.  
3) **Parser**: Wire parser child; add parser interface/strategies; apply post-provider with error handling.  
4) **Schemas**: Update cluster/tools/parser schemas per above; fix provider enum typo.  
5) **Docs**: Replace AI README cluster section with this content (or link to this file).  
6) **Tests**:
   - Unit: ToolRouter selection & node-as-tool wiring; memory backend load/append; parser strategies and fallback; child override resolution.  
   - Integration smoke: workflow similar to `templates/gemini_workflow_def.json` but with tools+memory+parser configured to ensure passthrough.

---

## References
- Current flow example: `src/main/resources/templates/gemini_workflow_def.json`
- Cluster executor: `AiClusterExecutor` (provider call, history, outputs)
- Request build: `AiExecutionRequestFactory`
- Provider registry: `AiClusterProviderResolver`
- Compound children: `AiClusterCompoundDescriptorProvider` (tools/context/parser/provider materialization)
