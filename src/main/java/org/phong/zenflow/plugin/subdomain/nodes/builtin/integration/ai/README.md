# AI Node Implementation

This implementation provides a flexible AI node system with support for various AI model providers, starting with Google's Gemini.

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

### Gemini Implementation

The Gemini implementation demonstrates the extensibility of the base architecture:

- **GeminiModelProvider** - Wraps Vertex AI Gemini chat model
- **GeminiAiExecutor** - Configures base executor with Gemini provider
- **GeminiResourceManager** - Manages Vertex AI client connections with observation
- **GeminiAiNode** - Plugin node definition

## Profile Configuration

The AI plugin uses a **GCP Credentials Profile** for authentication:

### Profile: `gcp-credentials`

```json
{
  "GCP_PROJECT_ID": "your-project-id",
  "GCP_LOCATION": "us-central1",
  "GCP_SERVICE_ACCOUNT_JSON": "optional-service-account-key"
}
```

**Fields:**
- `GCP_PROJECT_ID` (required): Your Google Cloud Platform project ID
- `GCP_LOCATION` (optional, default: "us-central1"): GCP region for Vertex AI
- `GCP_SERVICE_ACCOUNT_JSON` (optional): Service account JSON key. If not provided, uses Application Default Credentials

## Usage

### Basic Text Response

```yaml
nodes:
  - id: ai_chat
    type: integration:ai.gemini
    config:
      project_id: "your-gcp-project"
      location: "us-central1"
      model: "gemini-1.5-flash"
      prompt: "Explain quantum computing in simple terms"
      response_format: "text"
```

### JSON Response

```yaml
nodes:
  - id: ai_json
    type: integration:ai.gemini
    config:
      project_id: "your-gcp-project"
      model: "gemini-1.5-pro"
      prompt: "Generate a user profile with name, age, and interests"
      system_prompt: "You are a JSON generator. Only output valid JSON."
      response_format: "json"
      model_options:
        temperature: 0.3
        max_tokens: 1000
```

### With Model Options

```yaml
nodes:
  - id: ai_creative
    type: integration:ai.gemini
    config:
      project_id: "your-gcp-project"
      prompt: "Write a creative story about a robot"
      model_options:
        temperature: 0.9
        top_p: 0.95
        top_k: 40
        max_tokens: 2000
```

## Configuration Options

### Required
- `project_id` - GCP project ID for Vertex AI
- `prompt` - User prompt to send to the model

### Optional
- `location` - GCP region (default: "us-central1")
- `model` - Gemini model name (default: "gemini-1.5-flash")
- `system_prompt` - System message to guide the AI behavior
- `response_format` - "text" or "json" (default: "text")
- `model_options` - Map of model-specific options:
  - `temperature` - Sampling temperature (0.0 to 1.0)
  - `max_tokens` - Maximum tokens in response
  - `top_p` - Nucleus sampling threshold
  - `top_k` - Top-k sampling parameter

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
implementation platform('org.springframework.ai:spring-ai-bom:1.0.0-M4')
implementation 'org.springframework.ai:spring-ai-vertex-ai-gemini'
```

## Features

✅ Support for multiple AI providers
✅ Text and JSON response formats
✅ System and user message support
✅ Model-specific options (temperature, tokens, etc.)
✅ Token usage tracking
✅ Resource pooling for efficient connection management
✅ Lambda injection for provider customization
✅ Extensible architecture for future enhancements

## Future Enhancements

- Tool/Function calling support
- Multi-turn conversations with history
- Streaming responses
- Image and multimodal inputs
- Additional providers (OpenAI, Anthropic, Cohere, etc.)
- Response validation and retry logic
- Cost tracking and budget limits
