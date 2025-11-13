package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.openai.chatgpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiObservationRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

/**
 * Executor that adapts the base AI executor to OpenAI's chat completions API.
 */
@Component
@Slf4j
public class ChatgptExecutor implements NodeExecutor {

    private final AiExecutor baseExecutor;
    private final AiToolRegistry toolRegistry;
    private final AiObservationRegistry observationRegistry;

    public ChatgptExecutor(ObjectMapper objectMapper,
                           AiToolRegistry baseToolRegistry,
                           AiObservationRegistry baseObservationRegistry) {
        this.toolRegistry = baseToolRegistry.copy();
        this.observationRegistry = baseObservationRegistry.copy();

        log.info("OpenAI executor initialized with {} tools (independent copy)", this.toolRegistry.size());

        this.baseExecutor = new AiExecutor(objectMapper, this.toolRegistry, this.observationRegistry);
        this.baseExecutor.setModelProviderFactory(context -> {
            OpenAiChatModel chatModel = context.getResource();
            return new ChatgptModelProvider(chatModel, objectMapper);
        });
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        return baseExecutor.execute(context);
    }

    public AiToolRegistry getToolRegistry() {
        return baseExecutor.getToolRegistry();
    }
}
