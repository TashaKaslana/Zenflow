package org.phong.zenflow.plugin.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.plugin.dto.CreatePluginNode;
import org.phong.zenflow.plugin.dto.UpdatePluginNodeRequest;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.services.PluginNodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plugins/{pluginId}/nodes")
@RequiredArgsConstructor
public class PluginNodeController {

    private final PluginNodeService pluginNodeService;

    @PostMapping
    public ResponseEntity<RestApiResponse<PluginNode>> createPluginNode(
            @PathVariable UUID pluginId,
            @Valid @RequestBody CreatePluginNode request) {
        PluginNode createdNode = pluginNodeService.createPluginNode(pluginId, request);
        return RestApiResponse.created(createdNode, "Plugin node created successfully");
    }

    @GetMapping("/{nodeId}")
    public ResponseEntity<RestApiResponse<PluginNode>> getPluginNodeById(@PathVariable UUID nodeId) {
        PluginNode node = pluginNodeService.findById(nodeId);
        return RestApiResponse.success(node, "Plugin node retrieved successfully");
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RestApiResponse<PluginNode>> getPluginNodeByName(@PathVariable String name) {
        PluginNode node = pluginNodeService.findPluginNodeByName(name);
        return RestApiResponse.success(node, "Plugin node retrieved successfully");
    }

    @PutMapping("/{nodeId}")
    public ResponseEntity<RestApiResponse<PluginNode>> updatePluginNode(
            @PathVariable UUID nodeId,
            @Valid @RequestBody UpdatePluginNodeRequest request) {
        PluginNode updatedNode = pluginNodeService.updatePluginNode(nodeId, request);
        return RestApiResponse.success(updatedNode, "Plugin node updated successfully");
    }

    @DeleteMapping("/{nodeId}")
    public ResponseEntity<RestApiResponse<Void>> deletePluginNode(@PathVariable UUID nodeId) {
        pluginNodeService.deletePluginNode(nodeId);
        return RestApiResponse.success("Plugin node deleted successfully");
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<RestApiResponse<Void>> deletePluginNodes(@RequestBody List<UUID> nodeIds) {
        pluginNodeService.deletePluginNodes(nodeIds);
        return RestApiResponse.success("Plugin nodes deleted successfully");
    }
}
