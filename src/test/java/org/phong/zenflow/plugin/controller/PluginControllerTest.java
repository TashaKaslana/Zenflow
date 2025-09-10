package org.phong.zenflow.plugin.controller;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.plugin.infrastructure.mapstruct.PluginMapper;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.services.PluginService;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PluginControllerTest {

    @Test
    void shouldReturnProfileSchema() {
        PluginRepository repo = mock(PluginRepository.class);
        PluginMapper mapper = mock(PluginMapper.class);
        PluginService service = new PluginService(mapper, repo);
        PluginController controller = new PluginController(service);

        Plugin plugin = new Plugin();
        when(repo.findByKey("test")).thenReturn(Optional.of(plugin));

        ResponseEntity<RestApiResponse<Map<String, Object>>> response = controller.getPluginProfileSchema("test");

        assertNotNull(response.getBody());
        assertEquals("Test Profile", response.getBody().getData().get("title"));
    }
}
