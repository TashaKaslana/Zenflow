package org.phong.zenflow.workflow.subdomain.engine.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.utils.JsonSchemaValidator;
import org.phong.zenflow.plugin.subdomain.node.utils.SchemaRegistry;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.NodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final SchemaRegistry schemaRegistry;
    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;
    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final PluginNodeRepository pluginNodeRepository;
    private final SecretService secretService;
    private final NodeExecutorRegistry nodeExecutorRegistry;

    public void runWorkflow(UUID workflowId) {
        Map<String, Object> secretOfWorkflow = ObjectConversion.convertObjectToMap(secretService.getSecretsByWorkflowId(workflowId));
        Map<String, Object> context = new ConcurrentHashMap<>(Map.of("secrets", secretOfWorkflow));

        try {
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow(
                    () -> new WorkflowEngineException("Workflow not found with ID: " + workflowId)
            );

            validateWorkflowPluginNodeSchema(workflow);

            Map<String, Object> definition = workflow.getDefinition();
            List<BaseWorkflowNode> workflowSchema = objectMapper.readValue((JsonParser) definition.get("nodes"), new TypeReference<>() {
            });

            String startNodeKey = (String) definition.get("start");
            BaseWorkflowNode workingNode = workflowSchema.stream()
                    .filter(node -> node.getKey().equals(startNodeKey))
                    .findFirst()
                    .orElseThrow(() -> new WorkflowEngineException("Start node not found: " + startNodeKey));
            while (workingNode != null) {
                if (workingNode.getType() == NodeType.PLUGIN) {
                    workingNode = getNextNodeAfterPluginNode(workingNode, startNodeKey, context, workflowSchema);
                } else {
                    ExecutionResult result = nodeExecutorRegistry.execute(workingNode, context);
                    context.put(workingNode.getKey(), result.getOutput());

                    String nextNodeKey = result.getNextNodeKey();
                    if (nextNodeKey == null) {
                        log.info("Workflow completed successfully with ID: {}", workflowId);
                        break; // Exit loop if no next node is defined
                    }
                    workingNode = workflowSchema.stream().filter(node -> node.getKey().equals(nextNodeKey)).findFirst().orElse(null);
                }
            }
        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            throw new WorkflowEngineException("Error running workflow", e);
        }
    }

    private BaseWorkflowNode getNextNodeAfterPluginNode(BaseWorkflowNode workingNode, String startNodeKey,
                                                        Map<String, Object> context, List<BaseWorkflowNode> workflowSchema) {
        PluginDefinition pluginDefinition = (PluginDefinition) workingNode;
        PluginNode pluginNode = pluginNodeRepository.findById(pluginDefinition.getPluginNode().pluginId()).orElseThrow(
                () -> new WorkflowEngineException("Plugin node not found with ID: " + startNodeKey)
        );
        ExecutionResult result = executorDispatcher.dispatch(pluginNode, workingNode.getConfig(), context);
        //Random UUID will be used as a key of a result for temporary approaching, consider using a more meaningful key later
        context.put(workingNode.getKey(), result.getOutput());

        String nextNodeKey = workingNode.getNext().getFirst(); // Assuming single next node for simplicity
        workingNode = workflowSchema.stream().filter(node -> node.getKey().equals(nextNodeKey)).findFirst().orElse(null);
        return workingNode;
    }

    //TODO: navigator nodes don't get validation, review this later
    public void validateWorkflowPluginNodeSchema(@NotNull Workflow workflow) {
        try {
            Map<String, Object> definition = workflow.getDefinition();
            List<BaseWorkflowNode> workflowSchema = objectMapper.readValue((JsonParser) definition.get("nodes"), new TypeReference<>() {
            });

            for (BaseWorkflowNode nodeSchema : workflowSchema) {
                NodeType nodeType = nodeSchema.getType();
                JSONObject config = new JSONObject(nodeSchema.getConfig().toString());
                JSONObject nodeConfigSchema;

                if (Objects.requireNonNull(nodeType) == NodeType.PLUGIN) {
                    PluginNodeDefinition pluginNodeDefinition = ((PluginDefinition) nodeSchema).getPluginNode();
                    nodeConfigSchema = schemaRegistry.getPluginSchema(pluginNodeDefinition.pluginName(), pluginNodeDefinition.nodeName());
                } else {
                    log.warn("Unknown node type: {}", nodeType);
                    return;
                }

                JsonSchemaValidator.validate(config, nodeConfigSchema);
            }
        } catch (Exception e) {
            log.error("Error validating plugin node schema for workflow ID: {}", workflow.getId(), e);
            log.warn("Error validating plugin node schema for workflow ID: {}", workflow.getId(), e);
            throw new WorkflowEngineException("Error validating plugin node schema", e);
        }
    }
}
