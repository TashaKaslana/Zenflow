# AI Node Implementation

This implementation provides a flexible AI node system with support for various AI model providers (OpenAI and Google GenAI).  
We now ship the following AI nodes:

- **OpenAI ChatGPT** &mdash; default OpenAI node backed by the Chat Completions API (or any compatible host).
- **Gemini AI** &mdash; unified node backed by Spring AI Google GenAI (Gemini Developer API via API key or Vertex AI via GCP credentials).

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

### Provider Implementations

#### OpenAI ChatGPT (default)
- **OpenAiModelProvider** - Wraps the native OpenAI REST client with text/JSON parsing
- **OpenAiChatgptExecutor** - Configures the base executor with the OpenAI provider
- **OpenAiResourceManager** - Creates pooled OpenAI chat clients from host + API key secrets
- **OpenAiChatgptNode** - Plugin definition exposed under `openai:chatgpt`

#### Gemini (Google GenAI)
- **GeminiModelProvider** - Wraps the Spring AI Google GenAI chat model
- **GeminiAiExecutor** - Configures the base executor with the Gemini provider
- **GeminiResourceManager** - Builds Google GenAI clients for API key or Vertex AI credentials
- **GeminiAiNode** - Plugin definition exposed under `google-ai:gemini`

## Profile Configuration

All Gemini nodes use the **`gemini-ai-credentials`** profile descriptor:

```json
{
  "API_KEY": "sk-gemini-...",
  "BASE_URL": "https://generativelanguage.googleapis.com/v1beta"
}
```

**Common fields**
- `API_KEY` (required for API-key mode) &mdash; Gemini Developer API key.
- `BASE_URL` (optional) &mdash; Override host for Google GenAI requests.

**Vertex-only fields**
- `PROJECT_ID` &mdash; GCP project id that hosts Vertex AI.
- `REGION` &mdash; Vertex region (default `us-central1`).
- `SERVICE_ACCOUNT_JSON` or (`CLIENT_ID`, `CLIENT_SECRET`, `REFRESH_TOKEN`) &mdash; credentials used to obtain access tokens.

## Usage

### OpenAI ChatGPT (default)

#### Basic Text Response
```yaml
nodes:
  - id: chatgpt_text
    type: openai:chatgpt
    config:
      model: gpt-4o-mini
      prompt: "Summarise the latest release notes in two paragraphs"
```

#### JSON Response
```yaml
nodes:
  - id: chatgpt_json
    type: openai:chatgpt
    config:
      prompt: "Return a JSON object containing a random team name and mascot"
      system_prompt: "Only respond with valid JSON."
      response_format: json
      model_options:
        temperature: 0.2
        max_tokens: 800
```

### Gemini (Google GenAI)

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
      api_host: https://my-gemini-proxy.example.com/v1beta
```

#### Vertex AI Mode
```yaml
nodes:
  - id: gemini_vertex
    type: google-ai:gemini
    config:
      model: "gemini-1.5-pro"
      prompt: "Summarise today's revenue numbers"
```

## Configuration Options

### Gemini (Google GenAI) Node
- `prompt` (required) &mdash; user message sent to Gemini.
- `model` &mdash; defaults to `gemini-2.0-flash`.
- `system_prompt` &mdash; optional system instruction.
- `response_format` &mdash; `text` (default) or `json`.
- `api_host` &mdash; override host if the profile BASE_URL should not be used.
- `model_options` &mdash; map of Google GenAI settings (temperature, max_tokens/max_output_tokens, top_p, top_k, penalties, candidate_count, response_mime_type).

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
implementation platform("org.springframework.ai:spring-ai-bom:1.1.2")
implementation "org.springframework.ai:spring-ai-starter-model-openai"
implementation "org.springframework.ai:spring-ai-starter-model-google-genai"
```

## Features

- Support for multiple AI providers
- Text and JSON response formats
- System and user message support
- Model-specific options (temperature, tokens, etc.)
- Token usage tracking
- Resource pooling for efficient connection management
- Lambda injection for provider customization
- Tool/function calling support
- Extensible architecture for future enhancements

## Future Enhancements

- Multi-turn conversations with history
- Streaming responses
- Image and multimodal inputs
- Additional providers (OpenAI, Anthropic, Cohere, etc.)
- Response validation and retry logic
- Cost tracking and budget limits

