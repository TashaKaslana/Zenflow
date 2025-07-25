package org.phong.zenflow.plugin.subdomain.node.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.plugin.subdomain.node.dto.CreatePluginNode;
import org.phong.zenflow.plugin.subdomain.node.dto.PluginNodeDto;
import org.phong.zenflow.plugin.subdomain.node.dto.UpdatePluginNodeRequest;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plugins/nodes")
@RequiredArgsConstructor
public class PluginNodeController {

    private final PluginNodeService pluginNodeService;

    @PostMapping("for/{pluginId}")
    public ResponseEntity<RestApiResponse<PluginNodeDto>> createPluginNode(
            @PathVariable UUID pluginId,
            @Valid @RequestBody CreatePluginNode request) {
        PluginNodeDto createdNode = pluginNodeService.createPluginNode(pluginId, request);
        return RestApiResponse.created(createdNode, "Plugin node created successfully");
    }

    @GetMapping("for/{pluginId}")
    public ResponseEntity<RestApiResponse<List<PluginNodeDto>>> getAllPluginNodesByPluginId(@ParameterObject Pageable pageable, @PathVariable UUID pluginId) {
        Page<PluginNodeDto> nodes = pluginNodeService.findAllByPluginId(pageable, pluginId);
        return RestApiResponse.success(nodes, "Plugin nodes retrieved successfully");
    }

    @GetMapping("/{nodeId}")
    public ResponseEntity<RestApiResponse<PluginNodeDto>> getPluginNodeById(@PathVariable UUID nodeId) {
        PluginNodeDto node = pluginNodeService.findPluginNodeById(nodeId);
        return RestApiResponse.success(node, "Plugin node retrieved successfully");
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RestApiResponse<PluginNodeDto>> getPluginNodeByName(@PathVariable String name) {
        PluginNodeDto node = pluginNodeService.findPluginNodeByName(name);
        return RestApiResponse.success(node, "Plugin node retrieved successfully");
    }

    @PutMapping("/{nodeId}")
    public ResponseEntity<RestApiResponse<PluginNodeDto>> updatePluginNode(
            @PathVariable UUID nodeId,
            @Valid @RequestBody UpdatePluginNodeRequest request) {
        PluginNodeDto updatedNode = pluginNodeService.updatePluginNode(nodeId, request);
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
