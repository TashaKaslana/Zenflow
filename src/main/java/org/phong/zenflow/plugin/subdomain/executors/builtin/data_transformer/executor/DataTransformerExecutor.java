package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.dto.TransformStep;
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
    private final ObjectMapper objectMapper;

    @Override
    public String key() {
        return "core.data_transformer";
    }

    @Override
    public ExecutionResult execute(Map<String, Object> config, Map<String, Object> context) {
        List<String> logs = new ArrayList<>();
        log.debug("Executing DataTransformerExecutor with config: {}", config);
        logs.add("Executing DataTransformerExecutor with config: " + config);

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
            boolean isPipeline = Boolean.parseBoolean(config.getOrDefault("isPipeline", false).toString());
            String result = input;

            result = getResultTransform(config, isPipeline, result, transformerName, input, params, logs);

            return ExecutionResult.success(Map.of("result", result), logs);
        } catch (Exception e) {
            logs.add("Error occurred during data transformation: " + e.getMessage());
            log.debug("Data transformation failed {}", e.getMessage(), e);
            return ExecutionResult.error(e.getMessage(), logs);
        }
    }

    private String getResultTransform(Map<String, Object> config,
                                      boolean isPipeline,
                                      String result,
                                      String transformerName,
                                      String input,
                                      Map<String, Object> params,
                                      List<String> logs) throws JsonProcessingException {
        if (isPipeline) {
            Object stepsRaw = config.get("steps");
            if (stepsRaw == null) {
                logs.add("Pipeline steps are missing in the configuration.");
                log.debug("Pipeline steps are missing in the configuration.");
                throw new DataTransformerExecutorException("Pipeline steps are missing in the configuration.");
            }

            List<TransformStep> steps = objectMapper.readValue(
                    stepsRaw.toString(),
                    new TypeReference<>() {
                    }
            );

            for (TransformStep step : steps) {
                result = registry.getTransformer(step.getTransformer()).transform(result, step.getParams());
                logs.add(String.format("Applied transformer '%s' with params %s", step.getTransformer(), step.getParams()));
            }
        } else {
            result = registry.getTransformer(transformerName).transform(input, params);
            logs.add(String.format("Applied transformer '%s' with params %s", transformerName, params));
        }
        return result;
    }
}
