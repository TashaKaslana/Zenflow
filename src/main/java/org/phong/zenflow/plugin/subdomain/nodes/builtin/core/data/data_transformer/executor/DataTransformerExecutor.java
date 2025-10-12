package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.dto.TransformStep;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.data_transformer.registry.TransformerRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class DataTransformerExecutor implements NodeExecutor {
    private final TransformerRegistry registry;
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logPublisher = context.getLogPublisher();
        log.debug("Executing DataTransformerExecutor with config: {}", context.getCurrentConfig());
        logPublisher.info("Executing DataTransformerExecutor with config: " + context.getCurrentConfig());

        String transformerName = context.read("name", String.class);

        Object inputValue = context.read("data", Object.class);
        if (inputValue == null) {
            log.debug("Data is missing in the configuration.");
            logPublisher.error("Data is missing in the configuration.");
            throw new DataTransformerExecutorException("Data is missing in the configuration.");
        }

        Map<String, Object> params = ObjectConversion.convertObjectToMap(context.read("params", Object.class));
        // Defaults to single-transform mode when "isPipeline" flag is absent
        boolean isPipeline = Boolean.TRUE.equals(context.read("isPipeline", Object.class));
        boolean forEach = Boolean.TRUE.equals(context.read("forEach", Object.class));
        Object result;

        result = getResult(context, forEach, inputValue, logPublisher, isPipeline, transformerName, params);

        return ExecutionResult.success(Map.of("result", result));
    }

    private Object getResult(ExecutionContext context,
                             boolean forEach,
                             Object inputValue,
                             NodeLogPublisher logPublisher,
                             boolean isPipeline,
                             String transformerName,
                             Map<String, Object> params) {
        Object result;
        if (forEach) {
            if (!(inputValue instanceof List)) {
                logPublisher.error("Input must be a List when 'forEach' is true.");
                throw new DataTransformerExecutorException("Input must be a List when 'forEach' is true.");
            }
            logPublisher.info(String.format("Executing pipeline for each of %d items.", ((List<?>) inputValue).size()));
            List<Object> resultList = new ArrayList<>();
            for (Object item : (List<?>) inputValue) {
                Object transformedItem = getResultTransform(context, isPipeline, item, transformerName, params, logPublisher);
                resultList.add(transformedItem);
            }
            result = resultList;

        } else {
            result = getResultTransform(context, isPipeline, inputValue, transformerName, params, logPublisher);
        }
        logPublisher.success("Data transformation completed successfully.");
        return result;
    }

    private Object getResultTransform(ExecutionContext context,
                                      boolean isPipeline,
                                      Object inputValue,
                                      String transformerName,
                                      Map<String, Object> params,
                                      NodeLogPublisher logPublisher) {
        Object result = inputValue;
        if (isPipeline) {
            Object stepsRaw = context.read("steps", Object.class);
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
                LogContextManager.push(step.getTransformer());
                result = registry.getTransformer(step.getTransformer()).transform(result, step.getParams());
                logPublisher.info(String.format("Applied transformer '%s' with params %s", step.getTransformer(), step.getParams()));
                LogContextManager.pop();
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
