package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.executor;

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
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

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
        return "core:data.transformer";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config) {
        LogCollector logs = new LogCollector();
        log.debug("Executing DataTransformerExecutor with config: {}", config);
        logs.info("Executing DataTransformerExecutor with config: " + config);

        try {
            Map<String, Object> input = ObjectConversion.convertObjectToMap(config.input());
            String transformerName = (String) input.get("name");
            String inputValue = (String) input.get("input");

            if (inputValue == null || inputValue.trim().isEmpty()) {
                log.debug("Input data is missing in the configuration.");
                logs.error("Input data is missing in the configuration.");
                throw new DataTransformerExecutorException("Input data is missing in the configuration.");
            }

            Map<String, Object> params = ObjectConversion.convertObjectToMap(input.get("params"));
            boolean isPipeline = Boolean.parseBoolean(input.getOrDefault("isPipeline", false).toString());
            String result;

            result = getResultTransform(input, isPipeline, inputValue, transformerName, params, logs);
            logs.success("Data transformation completed successfully.");

            return ExecutionResult.success(Map.of("result", result), logs.getLogs());
        } catch (Exception e) {
            logs.error("Error occurred during data transformation: " + e.getMessage());
            log.debug("Data transformation failed {}", e.getMessage(), e);
            return ExecutionResult.error(e.getMessage(), logs.getLogs());
        }
    }

    private String getResultTransform(Map<String, Object> input,
                                      boolean isPipeline,
                                      String inputValue,
                                      String transformerName,
                                      Map<String, Object> params,
                                      LogCollector logs) {
        String result = inputValue;
        if (isPipeline) {
            Object stepsRaw = input.get("steps");
            if (stepsRaw == null) {
                logs.error("Pipeline steps are missing in the configuration.");
                log.debug("Pipeline steps are missing in the configuration.");
                throw new DataTransformerExecutorException("Pipeline steps are missing in the configuration.");
            }

            List<TransformStep> steps = objectMapper.convertValue(stepsRaw, new TypeReference<>() {});

            for (TransformStep step : steps) {
                result = registry.getTransformer(step.getTransformer()).transform(result, step.getParams());
                logs.info(String.format("Applied transformer '%s' with params %s", step.getTransformer(), step.getParams()));
            }
        } else {
            if (transformerName == null || transformerName.trim().isEmpty()) {
                log.debug("Transformer name is missing in the configuration.");
                logs.error("Transformer name is missing in the configuration.");
                throw new DataTransformerExecutorException("Transformer name is missing in the configuration.");
            }
            result = registry.getTransformer(transformerName).transform(inputValue, params);
            logs.info(String.format("Applied transformer '%s' with params %s", transformerName, params));
        }
        return result;
    }
}