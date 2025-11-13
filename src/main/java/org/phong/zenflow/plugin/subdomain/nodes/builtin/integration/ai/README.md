# AI Node Implementation

This implementation provides a flexible AI node system with support for various AI model providers, starting with Google's Gemini.  
We now ship two Gemini nodes:

- **Gemini AI** &mdash; default node backed by the public Gemini API exposed through the OpenAI-compatible protocol (host + API key).
- **Gemini AI (Vertex)** &mdash; legacy node that talks to Google Vertex AI with full GCP credentials.

## Architecture

### Base Components

1. **AiModelProvider** - Interface for all AI model providers
   - Abstracts the underlying model implementation
   - Supports tool calling and various response formats
   - Provider-specific options can be passed through

2. **AiExecutor** - Base executor for AI operations
   - Uses a factory pattern to inject model providers
   - Supports both text and JSON response formats
   - Handles conversation messages (system and user prompts)
   - Tracks token usage and metadata

3. **AiObservationRegistry** - Monitoring and observability
   - Provides logging-based observation for AI operations
   - Integrates with Micrometer for metrics and tracing
   - Tracks operation start, completion, and errors

### Gemini Implementations

#### Gemini API (default)
- **GeminiModelProvider** - Wraps the OpenAI-compatible Gemini chat model
- **GeminiAiExecutor** - Configures the base executor with the Gemini API provider
- **GeminiResourceManager** - Builds OpenAI clients from host + API key secrets
- **GeminiAiNode** - Plugin definition exposed under `google-ai:gemini`

#### Gemini Vertex (legacy)
- **GeminiVertexModelProvider** - Wraps the Vertex AI chat model
- **GeminiVertexAiExecutor** - Configures the base executor with the Vertex provider
- **GeminiVertexResourceManager** - Manages Vertex AI clients and pooling
- **GeminiVertexAiNode** - Plugin definition exposed under `google-ai:gemini-vertex`

## Profile Configuration

Both nodes reuse the **`ai-credentials`** profile descriptor:

```json
{
  "API_KEY": "sk-gemini-...",
  "BASE_URL": "https://generativelanguage.googleapis.com/v1beta/openai/"
}
```

**Common fields**
- `API_KEY` (required) &mdash; API key for the Gemini API (or any OpenAI-compatible host).
- `BASE_URL` (optional) &mdash; Override host. Defaults to `https://generativelanguage.googleapis.com/v1beta/openai/`.

**Vertex-only fields**
- `PROJECT_ID` &mdash; GCP project id that hosts Vertex AI.
- `REGION` &mdash; Vertex region (default `us-central1`).
- `SERVICE_ACCOUNT_JSON` or (`CLIENT_ID`, `CLIENT_SECRET`, `REFRESH_TOKEN`) &mdash; credentials used to obtain access tokens.

## Usage

### Gemini API (default)

#### Basic Text Response
```yaml
nodes:
  - id: gemini_chat
    type: google-ai:gemini
    config:
      model: gemini-2.0-flash
      prompt: "Explain quantum computing in simple terms"
      response_format: text
```

#### JSON Response
```yaml
nodes:
  - id: gemini_json
    type: google-ai:gemini
    config:
      prompt: "Generate a user profile with name, age, and interests"
      system_prompt: "You are a JSON generator. Only output valid JSON."
      response_format: json
      model_options:
        temperature: 0.3
        max_tokens: 1000
```

#### Custom Host
```yaml
nodes:
  - id: gemini_router
    type: google-ai:gemini
    config:
      prompt: "Route this request through my custom host"
      api_host: https://my-gemini-proxy.example.com/v1beta/openai/
```

### Gemini Vertex (legacy)

```yaml
nodes:
  - id: gemini_vertex
    type: google-ai:gemini-vertex
    config:
      project_id: "your-gcp-project"
      location: "us-central1"
      model: "gemini-1.5-pro"
      prompt: "Summarise today's revenue numbers"
```

## Configuration Options

### Gemini API Node
- `prompt` (required) &mdash; user message sent to Gemini.
- `model` &mdash; defaults to `gemini-2.0-flash`.
- `system_prompt` &mdash; optional system instruction.
- `response_format` &mdash; `text` (default) or `json`.
- `api_host` &mdash; override host if the profile BASE_URL should not be used.
- `model_options` &mdash; map of OpenAI-compatible settings (temperature, max_tokens, top_p, penalties, raw `response_format`, etc.).

### Gemini Vertex Node
- `project_id` (required) &mdash; Vertex project id.
- `location` &mdash; Vertex region (`us-central1` default).
- `prompt` (required).
- Same `model`, `system_prompt`, `response_format`, and `model_options` knobs as above, plus Vertex-only `top_k`.

## Output Context Variables

- `response` - Processed response (String for text, Object for JSON)
- `raw_response` - Raw text response from the model
- `provider` - Provider name ("gemini")
- `usage` - Token usage statistics:
  - `prompt_tokens` - Tokens in the prompt
  - `completion_tokens` - Tokens in the response
  - `total_tokens` - Total tokens used

## Adding New Providers

To add support for other AI providers (OpenAI, Anthropic, etc.):

1. Implement `AiModelProvider` interface
2. Create a provider-specific executor extending the base
3. Create a resource manager for connection pooling
4. Register as a `@PluginNode`

Example for OpenAI:

```java
public class OpenAiModelProvider implements AiModelProvider {
    private final OpenAiChatModel chatModel;
    
    @Override
    public ChatResponse call(Prompt prompt, Map<String, Object> options) {
        // Convert options to OpenAI-specific format
        OpenAiChatOptions chatOptions = buildOptions(options);
        return chatModel.call(new Prompt(prompt.getInstructions(), chatOptions));
    }
    
    @Override
    public String getProviderName() {
        return "openai";
    }
}
```

## Dependencies

This implementation uses Spring AI:
```gradle
implementation platform("org.springframework.ai:spring-ai-bom:1.0.3")
implementation "org.springframework.ai:spring-ai-starter-model-openai"
implementation "org.springframework.ai:spring-ai-vertex-ai-gemini"
```

## Features

- Support for multiple AI providers
- Text and JSON response formats
- System and user message support
- Model-specific options (temperature, tokens, etc.)
- Token usage tracking
- Resource pooling for efficient connection management
- Lambda injection for provider customization
- Extensible architecture for future enhancements

## Future Enhancements

- Tool/Function calling support
- Multi-turn conversations with history
- Streaming responses
- Image and multimodal inputs
- Additional providers (OpenAI, Anthropic, Cohere, etc.)
- Response validation and retry logic
- Cost tracking and budget limits

