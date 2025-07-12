package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@AllArgsConstructor
public class PluginNodeExecutorDispatcher {

    private final PluginNodeExecutorRegistry registry;
    private final WebClient webClient;

    public ExecutionResult dispatch(PluginNode node, Map<String, Object> config, Map<String, Object> context) {
        String key = node.getPlugin().getName() + ":" + node.getName();
        context = TemplateEngine.resolveAll(config, context);

        switch (node.getExecutorType().toLowerCase()) {
            case "builtin" -> {
                return registry.getExecutor(key)
                        .orElseThrow(() -> new ExecutorException("Executor not found: " + key))
                        .execute(config, context);
            }
            case "remote" -> {
                return webClient.post()
                        .uri(node.getEntrypoint())
                        .bodyValue(Map.of("config", config, "input", context))
                        .retrieve()
                        .bodyToMono(ExecutionResult.class)
                        .block();
            }
            default ->  throw new ExecutorException("Unknown executor type: " + node.getExecutorType());
        }
    }
}
