package org.phong.zenflow.workflow.subdomain.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class WorkflowContextService {
    @Data
    public static class StaticContext {
        private Map<String, Set<String>> nodeDependency = new HashMap<>();
        private Map<String, OutputUsage> nodeConsumerFlat = new HashMap<>();
        private Map<String, String> alias = new HashMap<>();
    }

    @Data
    public static class OutputUsage {
        private String type;
        private List<String> consumers = new ArrayList<>();
        private List<String> alias = new ArrayList<>();
    }

    public StaticContext buildStaticContext(List<BaseWorkflowNode> nodes) {
        StaticContext ctx = new StaticContext();

        // First pass: collect aliases from outputs
        for (BaseWorkflowNode node : nodes) {
            Map<String, Object> output = node.getConfig().output();
            for (Map.Entry<String, Object> entry : output.entrySet()) {
                String val = entry.getValue().toString();
                if (TemplateUtils.isTemplate(val)) {
                    List<String> refs = TemplateUtils.extractRefs(val);
                    if (refs.size() == 1) {
                        // Simple alias: output key -> referenced value
                        String aliasKey = node.getKey() + ".output." + entry.getKey();
                        ctx.getAlias().put(aliasKey, val);

                        // Track alias in consumer metadata
                        String sourceRef = refs.getFirst();
                        ctx.getNodeConsumerFlat()
                                .computeIfAbsent(sourceRef, k -> new OutputUsage())
                                .getAlias()
                                .add(aliasKey);
                    }
                }
            }
        }

        // Second pass: process input dependencies
        for (BaseWorkflowNode node : nodes) {
            String nodeKey = node.getKey();
            Map<String, Object> input = node.getConfig().input();

            for (Map.Entry<String, Object> entry : input.entrySet()) {
                String inputValue = entry.getValue().toString();
                List<String> referenced = TemplateUtils.extractRefs(inputValue);

                for (String ref : referenced) {
                    // Add dependency
                    ctx.getNodeDependency()
                            .computeIfAbsent(nodeKey, k -> new HashSet<>())
                            .add(ref);

                    // Track consumer
                    ctx.getNodeConsumerFlat()
                            .computeIfAbsent(ref, k -> new OutputUsage())
                            .getConsumers()
                            .add(nodeKey);

                    // Infer type from input (basic type inference)
                    OutputUsage usage = ctx.getNodeConsumerFlat().get(ref);
                    if (usage.getType() == null) {
                        usage.setType(inferType(entry.getValue()));
                    }
                }
            }
        }

        return ctx;
    }

    private String inferType(Object value) {
        if (value instanceof Number) {
            return "number";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else {
            return "string";
        }
    }
}