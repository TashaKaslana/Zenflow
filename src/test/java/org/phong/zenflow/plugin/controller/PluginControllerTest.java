package org.phong.zenflow.plugin.controller;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.plugin.infrastructure.mapstruct.PluginMapper;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.services.PluginDescriptorService;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PluginControllerTest {

    @Test
    void shouldReturnProfileSchema() {
        PluginRepository repo = mock(PluginRepository.class);
        PluginMapper mapper = mock(PluginMapper.class);
        PluginService service = new PluginService(mapper, repo);
        PluginDescriptorService descriptorService = mock(PluginDescriptorService.class);
        PluginController controller = new PluginController(service, descriptorService);

        // Create a plugin with proper schema data
        Plugin plugin = new Plugin();

        // Set up the plugin schema with a profile section
        Map<String, Object> pluginSchema = new HashMap<>();
        Map<String, Object> profileSchema = new HashMap<>();
        profileSchema.put("title", "Test Profile");
        profileSchema.put("type", "object");
        profileSchema.put("properties", new HashMap<>());

        pluginSchema.put("profile", profileSchema);
        plugin.setPluginSchema(pluginSchema);

        when(repo.findByKey("test")).thenReturn(Optional.of(plugin));

        ResponseEntity<RestApiResponse<Map<String, Object>>> response = controller.getPluginProfileSchema("test");

        assertNotNull(response.getBody());
        assertEquals("Test Profile", response.getBody().getData().get("title"));
    }
}
