package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.executor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.registry.TransformerRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class DataTransformerExecutor implements PluginNodeExecutor {
    private final TransformerRegistry registry;

    @Override
    public String key() {
        return "Core:Data Transformer";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        List<String> logs = new ArrayList<>();
        try {
            String transformerName = (String) config.get("name");
            if (transformerName == null || transformerName.trim().isEmpty()) {
                log.debug("Transformer name is missing in the configuration.");
                logs.add("Transformer name is missing in the configuration.");
                throw new DataTransformerExecutorException("Transformer name is missing in the configuration.");
            }

            String input = (String) config.get("input");
            if (input == null || input.trim().isEmpty()) {
                log.debug("Input data is missing in the configuration.");
                logs.add("Input data is missing in the configuration.");
                throw new DataTransformerExecutorException("Input data is missing in the configuration.");
            }

            Map<String, Object> params = ObjectConversion.convertObjectToMap(config.get("params"));


            String result = registry.getTransformer(transformerName).transform(input, params);

            return ExecutionResult.success(Map.of("result", result), logs);
        } catch (Exception e) {
            logs.add("Error occurred during data transformation: " + e.getMessage());
            log.debug("Data transformation failed", e);
            return ExecutionResult.error(e.getMessage(), logs);
        }
    }
}
