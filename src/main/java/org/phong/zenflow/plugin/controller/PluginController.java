package org.phong.zenflow.plugin.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.plugin.dto.CreatePluginRequest;
import org.phong.zenflow.plugin.dto.PluginDto;
import org.phong.zenflow.plugin.dto.UpdatePluginRequest;
import org.phong.zenflow.plugin.services.PluginService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginService pluginService;

    @PostMapping
    public ResponseEntity<RestApiResponse<PluginDto>> createPlugin(@Valid @RequestBody CreatePluginRequest request) {
        PluginDto createdPlugin = pluginService.savePlugin(request);
        return RestApiResponse.created(createdPlugin, "Plugin uploaded successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<PluginDto>>> getAllPlugins(Pageable pageable) {
        Page<PluginDto> plugins = pluginService.getAllPlugins(pageable);
        return RestApiResponse.success(plugins, "Plugins retrieved successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<PluginDto>> getPluginById(@PathVariable UUID id) {
        PluginDto plugin = pluginService.getPluginById(id);
        return RestApiResponse.success(plugin, "Plugin retrieved successfully");
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RestApiResponse<PluginDto>> getPluginByName(@PathVariable String name) {
        PluginDto plugin = pluginService.getPluginByName(name);
        return RestApiResponse.success(plugin, "Plugin retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<PluginDto>> updatePlugin(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePluginRequest request) {
        PluginDto updatedPlugin = pluginService.updatePlugin(id, request);
        return RestApiResponse.success(updatedPlugin, "Plugin updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deletePlugin(@PathVariable UUID id) {
        pluginService.deletePlugin(id);
        return RestApiResponse.success("Plugin deleted successfully");
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<RestApiResponse<Void>> deletePlugins(@RequestBody List<UUID> ids) {
        pluginService.deletePluginsByIds(ids);
        return RestApiResponse.success("Plugins deleted successfully");
    }

      @DeleteMapping("/all")
      public ResponseEntity<RestApiResponse<Void>> deleteAllPlugins() {
          pluginService.deleteAllPlugins();
          return RestApiResponse.success("All plugins deleted successfully");
      }

      @GetMapping("/{key}/schema")
      public ResponseEntity<RestApiResponse<Map<String, Object>>> getPluginSchema(@PathVariable String key) {
          Map<String, Object> schema = pluginService.getPluginSchemaByKey(key);
          return RestApiResponse.success(schema, "Plugin schema retrieved successfully");
      }

      @GetMapping("/{key}/profile-schema")
      public ResponseEntity<RestApiResponse<Map<String, Object>>> getPluginProfileSchema(@PathVariable String key) {
          Map<String, Object> schema = pluginService.getProfileSchemaById(key);
          return RestApiResponse.success(schema, "Plugin profile schema retrieved successfully");
      }
  }
