package org.phong.zenflow.workflow.subdomain.context;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
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
    public Map<String, Object> buildStaticContext(List<BaseWorkflowNode> nodes, Map<String, Object> existingMetadata) {
        StaticContext ctx = new StaticContext();

        // Preserve existing aliases from metadata
        if (existingMetadata != null && existingMetadata.containsKey("alias")) {
            ctx.setAlias(ObjectConversion.safeConvert(existingMetadata.get("alias"), new TypeReference<>() {
            }));
        }

        // First pass: collect all output references and pre-populate consumer map
//        for (BaseWorkflowNode node : nodes) {
//            Map<String, Object> output = node.getConfig().output();
//            for (Map.Entry<String, Object> entry : output.entrySet()) {
//                String outputKey = node.getKey() + ".output." + entry.getKey();
//                ctx.getNodeConsumer().put(outputKey, new OutputUsage());
//            }
//        }

        // Second pass: process input dependencies to populate consumers and dependencies
        for (BaseWorkflowNode node : nodes) {
            String nodeKey = node.getKey();
            Map<String, Object> input = node.getConfig().input();
            if (input == null || input.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, Object> entry : input.entrySet()) {
                String inputValue = entry.getValue().toString();
                List<String> referenced = TemplateEngine.extractRefs(inputValue);

                for (String ref : referenced) {
                    String resolvedRef = resolveAlias(ref, ctx.getAlias());

                    // Add dependency
                    ctx.getNodeDependency()
                            .computeIfAbsent(nodeKey, k -> new HashSet<>())
                            .add(resolvedRef);

                    // Track consumer
                    ctx.getNodeConsumer()
                            .computeIfAbsent(resolvedRef, k -> new OutputUsage())
                            .getConsumers()
                            .add(nodeKey);

                    // Infer type from input (basic type inference)
                    OutputUsage usage = ctx.getNodeConsumer().get(resolvedRef);
                    if (usage.getType() == null) {
                        usage.setType(inferType(entry.getValue()));
                    }
                }
            }
        }

        // Third pass: link aliases back to the consumers map
        for (Map.Entry<String, String> aliasEntry : ctx.getAlias().entrySet()) {
            String aliasName = aliasEntry.getKey();
            TemplateEngine.extractRefs(aliasEntry.getValue()).stream().findFirst().ifPresent(originalRef -> ctx.getNodeConsumer()
                    .computeIfAbsent(originalRef, k -> new OutputUsage())
                    .getAlias()
                    .add(aliasName));
        }

        // Merge the generated context back into the existing metadata
        Map<String, Object> newMetadata = new HashMap<>(existingMetadata != null ? existingMetadata : new HashMap<>());
        newMetadata.put("nodeDependency", ctx.getNodeDependency());
        newMetadata.put("nodeConsumer", ctx.getNodeConsumer());
        newMetadata.put("alias", ctx.getAlias()); // Ensure aliases are preserved

        return newMetadata;
    }

    private String resolveAlias(String ref, Map<String, String> aliases) {
        if (aliases.containsKey(ref)) {
            return TemplateEngine.extractRefs(aliases.get(ref)).stream().findFirst().orElse(ref);
        }
        return ref;
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

    @Data
    public static class StaticContext {
        private Map<String, Set<String>> nodeDependency = new HashMap<>();
        private Map<String, OutputUsage> nodeConsumer = new HashMap<>();
        private Map<String, String> alias = new HashMap<>();
    }

    @Data
    public static class OutputUsage {
        private String type;
        private Set<String> consumers = new HashSet<>();
        private List<String> alias = new ArrayList<>();
    }
}