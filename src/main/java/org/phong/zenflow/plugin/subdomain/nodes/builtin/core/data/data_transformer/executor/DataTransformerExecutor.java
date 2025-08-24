package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.dto.TransformStep;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.registry.TransformerRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@PluginNode(
        key = "core:data.transformer",
        name = "Data Transformer",
        version = "1.0.0",
        description = "Executes data transformation using registered transformers. Supports both single-transform and pipeline modes.",
        type = "data_transformation",
        tags = {"data", "transformation", "pipeline"},
        icon = "ph:code",
        schemaPath = "../schema.json",
        docPath = "../doc.md"
)
@AllArgsConstructor
@Slf4j
public class DataTransformerExecutor implements PluginNodeExecutor {
    private final TransformerRegistry registry;

    @Override
    public String key() {
        return "core:data.transformer:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logPublisher = context.getLogPublisher();
        log.debug("Executing DataTransformerExecutor with config: {}", config);
        logPublisher.info("Executing DataTransformerExecutor with config: " + config);

        try {
            Object rawInput = config.input();
            if (rawInput == null) {
                log.debug("Configuration input is missing.");
                logPublisher.error("Configuration input is missing.");
                throw new DataTransformerExecutorException("Configuration input is missing.");
            }
            Map<String, Object> input = ObjectConversion.convertObjectToMap(rawInput);

            Object nameObj = input.get("name");
            if (nameObj != null && !(nameObj instanceof String)) {
                log.debug("Transformer name must be a string.");
                logPublisher.error("Transformer name must be a string.");
                throw new DataTransformerExecutorException("Transformer name must be a string.");
            }
            String transformerName = (String) nameObj;

            Object inputValue = input.get("data");
            if (inputValue == null) {
                log.debug("Data is missing in the configuration.");
                logPublisher.error("Data is missing in the configuration.");
                throw new DataTransformerExecutorException("Data is missing in the configuration.");
            }

            Map<String, Object> params = ObjectConversion.convertObjectToMap(input.get("params"));
            // Defaults to single-transform mode when "isPipeline" flag is absent
            boolean isPipeline = Boolean.TRUE.equals(input.get("isPipeline"));
            boolean forEach = Boolean.TRUE.equals(input.get("forEach"));
            Object result;

            if (forEach) {
                if (!(inputValue instanceof List)) {
                    logPublisher.error("Input must be a List when 'forEach' is true.");
                    throw new DataTransformerExecutorException("Input must be a List when 'forEach' is true.");
                }
                logPublisher.info(String.format("Executing pipeline for each of %d items.", ((List<?>) inputValue).size()));
                List<Object> resultList = new ArrayList<>();
                for (Object item : (List<?>) inputValue) {
                    Object transformedItem = getResultTransform(input, isPipeline, item, transformerName, params, logPublisher);
                    resultList.add(transformedItem);
                }
                result = resultList;

            } else {
                result = getResultTransform(input, isPipeline, inputValue, transformerName, params, logPublisher);
            }
            logPublisher.success("Data transformation completed successfully.");

            return ExecutionResult.success(Map.of("result", result));
        } catch (Exception e) {
            logPublisher.error("Error occurred during data transformation: " + e.getMessage());
            log.debug("Data transformation failed {}", e.getMessage(), e);
            return ExecutionResult.error(e.getMessage());
        }
    }

    private Object getResultTransform(Map<String, Object> input,
                                      boolean isPipeline,
                                      Object inputValue,
                                      String transformerName,
                                      Map<String, Object> params,
                                      NodeLogPublisher logPublisher) {
        Object result = inputValue;
        if (isPipeline) {
            Object stepsRaw = input.get("steps");
            if (stepsRaw == null) {
                logPublisher.error("Pipeline steps are missing in the configuration.");
                log.debug("Pipeline steps are missing in the configuration.");
                throw new DataTransformerExecutorException("Pipeline steps are missing in the configuration.");
            }

            List<TransformStep> steps = ObjectConversion.safeConvert(stepsRaw, new TypeReference<>() {
            });

            for (TransformStep step : steps) {
                if (step.getTransformer() == null || step.getTransformer().trim().isEmpty()) {
                    logPublisher.error("Transformer name is missing in a pipeline step.");
                    log.debug("Transformer name is missing in a pipeline step.");
                    throw new DataTransformerExecutorException("Transformer name is missing in a pipeline step.");
                }
                if (step.getParams() == null) {
                    logPublisher.error(String.format("Params are missing for transformer '%s'", step.getTransformer()));
                    log.debug("Params are missing for transformer '{}'", step.getTransformer());
                    throw new DataTransformerExecutorException(
                            "Params are missing for transformer: " + step.getTransformer());
                }
            }

            for (TransformStep step : steps) {
                result = registry.getTransformer(step.getTransformer()).transform(result, step.getParams());
                logPublisher.info(String.format("Applied transformer '%s' with params %s", step.getTransformer(), step.getParams()));
            }
        } else {
            if (transformerName == null || transformerName.trim().isEmpty()) {
                log.debug("Transformer name is missing in the configuration.");
                logPublisher.error("Transformer name is missing in the configuration.");
                throw new DataTransformerExecutorException("Transformer name is missing in the configuration.");
            }
            result = registry.getTransformer(transformerName).transform(inputValue, params);
            logPublisher.info(String.format("Applied transformer '%s' with params %s", transformerName, params));
        }
        return result;
    }
}