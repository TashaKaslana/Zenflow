package org.phong.zenflow.plugin.subdomain.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.phong.zenflow.plugin.subdomain.registry.definitions.CorePlugin;
import org.phong.zenflow.plugin.subdomain.registry.definitions.IntegrationPlugin;
import org.phong.zenflow.plugin.subdomain.registry.definitions.TestPlugin;
import org.phong.zenflow.plugin.subdomain.registry.definitions.GoogleDrivePlugin;
import org.phong.zenflow.plugin.subdomain.registry.definitions.GoogleDocsPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = {
        CorePlugin.class,
        IntegrationPlugin.class,
        TestPlugin.class,
        GoogleDrivePlugin.class,
        GoogleDocsPlugin.class,
        PluginSynchronizer.class,
        ObjectMapper.class
})
public class PluginTest {

    @MockitoBean
    private PluginRepository pluginRepository;

    @Autowired
    private PluginSynchronizer pluginSynchronizer;

    @Test
    void testCorePluginRegistration() {
        // Test that the core plugin is properly registered

        // Mock repository behavior
        Plugin mockPlugin = new Plugin();
        mockPlugin.setKey("core");
        mockPlugin.setName("Core Plugin");
        mockPlugin.setVersion("1.0.0");
        mockPlugin.setDescription("Built-in core plugin containing essential nodes for workflow execution including HTTP requests, data transformation, flow control, loops, and triggers.");
        mockPlugin.setTags(Arrays.asList("core", "builtin", "essential"));
        mockPlugin.setIcon("ph:core");
        mockPlugin.setVerified(true);
        mockPlugin.setPublisherId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        when(pluginRepository.findByKey("core")).thenReturn(Optional.empty());
        when(pluginRepository.save(any(Plugin.class))).thenReturn(mockPlugin);

        // Execute synchronizer
        assertDoesNotThrow(() -> pluginSynchronizer.run(null),
                "Plugin synchronization should not throw any exceptions");

        // Verify that repository save was called with correct plugin data
        verify(pluginRepository, atLeastOnce()).save(argThat(plugin ->
                "core".equals(plugin.getKey()) &&
                "Core Plugin".equals(plugin.getName()) &&
                "1.0.0".equals(plugin.getVersion()) &&
                plugin.getTags().contains("core") &&
                plugin.getTags().contains("builtin") &&
                plugin.getTags().contains("essential") &&
                "ph:core".equals(plugin.getIcon()) &&
                plugin.getVerified() == true &&
                UUID.fromString("00000000-0000-0000-0000-000000000000").equals(plugin.getPublisherId())
        ));
    }

    @Test
    void testIntegrationPluginRegistration() {
        // Test that the integration plugin is properly registered

        Plugin mockPlugin = new Plugin();
        mockPlugin.setKey("integration");
        mockPlugin.setName("Integration Plugin");
        mockPlugin.setVersion("1.0.0");
        mockPlugin.setDescription("Plugin containing integration nodes for external services including databases, email, and third-party APIs.");
        mockPlugin.setTags(Arrays.asList("integration", "database", "email", "external"));
        mockPlugin.setIcon("ph:plug");
        mockPlugin.setVerified(true);
        mockPlugin.setPublisherId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        when(pluginRepository.findByKey("integration")).thenReturn(Optional.empty());
        when(pluginRepository.save(any(Plugin.class))).thenReturn(mockPlugin);

        // Execute synchronizer
        assertDoesNotThrow(() -> pluginSynchronizer.run(null),
                "Plugin synchronization should not throw any exceptions");

        // Verify that repository save was called with correct plugin data
        verify(pluginRepository, atLeastOnce()).save(argThat(plugin ->
                "integration".equals(plugin.getKey()) &&
                "Integration Plugin".equals(plugin.getName()) &&
                "1.0.0".equals(plugin.getVersion()) &&
                plugin.getTags().contains("integration") &&
                plugin.getTags().contains("database") &&
                plugin.getTags().contains("email") &&
                plugin.getTags().contains("external") &&
                "ph:plug".equals(plugin.getIcon()) &&
                plugin.getVerified() == true
        ));
    }

    @Test
    void testTestPluginRegistration() {
        // Test that the test plugin is properly registered

        Plugin mockPlugin = new Plugin();
        mockPlugin.setKey("test");
        mockPlugin.setName("Test Plugin");
        mockPlugin.setVersion("1.0.0");
        mockPlugin.setDescription("Plugin containing test and development nodes including data generators, validators, and placeholder nodes.");
        mockPlugin.setTags(Arrays.asList("test", "development", "validation", "mock"));
        mockPlugin.setIcon("ph:test-tube");
        mockPlugin.setVerified(true);
        mockPlugin.setPublisherId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        when(pluginRepository.findByKey("test")).thenReturn(Optional.empty());
        when(pluginRepository.save(any(Plugin.class))).thenReturn(mockPlugin);

        // Execute synchronizer
        assertDoesNotThrow(() -> pluginSynchronizer.run(null),
                "Plugin synchronization should not throw any exceptions");

        // Verify that repository save was called with correct plugin data
        verify(pluginRepository, atLeastOnce()).save(argThat(plugin ->
                "test".equals(plugin.getKey()) &&
                "Test Plugin".equals(plugin.getName()) &&
                "1.0.0".equals(plugin.getVersion()) &&
                plugin.getTags().contains("test") &&
                plugin.getTags().contains("development") &&
                plugin.getTags().contains("validation") &&
                plugin.getTags().contains("mock") &&
                "ph:test-tube".equals(plugin.getIcon()) &&
                plugin.getVerified() == true
        ));
    }

    @Test
    void testAllPluginsRegistered() {
        // Test that all plugins are discovered and registered

        // Mock repository behavior for all plugins
        when(pluginRepository.findByKey(anyString())).thenReturn(Optional.empty());
        when(pluginRepository.save(any(Plugin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute synchronizer
        assertDoesNotThrow(() -> pluginSynchronizer.run(null),
                "Plugin synchronization should not throw any exceptions");

        // Verify that all five plugins were saved
        verify(pluginRepository, times(5)).save(any(Plugin.class));

        // Verify each plugin key was searched for
        verify(pluginRepository).findByKey("core");
        verify(pluginRepository).findByKey("integration");
        verify(pluginRepository).findByKey("test");
        verify(pluginRepository).findByKey("google-drive");
        verify(pluginRepository).findByKey("google-docs");
    }

    @Test
    void testPluginUpdateOnExisting() {
        // Test that existing plugins are updated with new information

        Plugin existingPlugin = new Plugin();
        existingPlugin.setKey("core");
        existingPlugin.setName("Old Core Plugin Name");
        existingPlugin.setVersion("0.9.0");
        existingPlugin.setDescription("Old description");

        when(pluginRepository.findByKey("core")).thenReturn(Optional.of(existingPlugin));
        when(pluginRepository.save(any(Plugin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute synchronizer
        assertDoesNotThrow(() -> pluginSynchronizer.run(null),
                "Plugin synchronization should not throw any exceptions");

        // Verify that the existing plugin was updated with new annotation values
        verify(pluginRepository).save(argThat(plugin ->
                "core".equals(plugin.getKey()) &&
                "Core Plugin".equals(plugin.getName()) && // Should be updated to new name
                "1.0.0".equals(plugin.getVersion()) && // Should be updated to new version
                plugin.getDescription().contains("Built-in core plugin") // Should be updated to new description
        ));
    }

    @Test
    @SuppressWarnings("unused")
    void testPluginAnnotationValidation() {
        // Test that plugin annotations contain all required fields

        // This test validates that our plugin definition classes have proper annotations
        CorePlugin corePlugin = new CorePlugin();
        IntegrationPlugin integrationPlugin = new IntegrationPlugin();
        TestPlugin testPlugin = new TestPlugin();

        // Verify that the classes have the @Plugin annotation
        assertTrue(CorePlugin.class.isAnnotationPresent(
                org.phong.zenflow.plugin.subdomain.registry.Plugin.class),
                "CorePlugin should have @Plugin annotation");

        assertTrue(IntegrationPlugin.class.isAnnotationPresent(
                org.phong.zenflow.plugin.subdomain.registry.Plugin.class),
                "IntegrationPlugin should have @Plugin annotation");

        assertTrue(TestPlugin.class.isAnnotationPresent(
                org.phong.zenflow.plugin.subdomain.registry.Plugin.class),
                "TestPlugin should have @Plugin annotation");

        // Verify annotation values for CorePlugin
        org.phong.zenflow.plugin.subdomain.registry.Plugin coreAnnotation =
                CorePlugin.class.getAnnotation(org.phong.zenflow.plugin.subdomain.registry.Plugin.class);

        assertEquals("core", coreAnnotation.key(), "Core plugin key should be 'core'");
        assertEquals("Core Plugin", coreAnnotation.name(), "Core plugin name should be 'Core Plugin'");
        assertEquals("1.0.0", coreAnnotation.version(), "Core plugin version should be '1.0.0'");
        assertFalse(coreAnnotation.description().isEmpty(), "Core plugin should have a description");
        assertTrue(coreAnnotation.tags().length > 0, "Core plugin should have tags");
        assertEquals("ph:core", coreAnnotation.icon(), "Core plugin should have correct icon");
        assertTrue(coreAnnotation.verified(), "Core plugin should be verified");

        // Verify annotation values for IntegrationPlugin
        org.phong.zenflow.plugin.subdomain.registry.Plugin integrationAnnotation =
                IntegrationPlugin.class.getAnnotation(org.phong.zenflow.plugin.subdomain.registry.Plugin.class);

        assertEquals("integration", integrationAnnotation.key(), "Integration plugin key should be 'integration'");
        assertEquals("Integration Plugin", integrationAnnotation.name(), "Integration plugin name should be 'Integration Plugin'");
        assertEquals("1.0.0", integrationAnnotation.version(), "Integration plugin version should be '1.0.0'");
        assertFalse(integrationAnnotation.description().isEmpty(), "Integration plugin should have a description");
        assertTrue(integrationAnnotation.tags().length > 0, "Integration plugin should have tags");
        assertEquals("ph:plug", integrationAnnotation.icon(), "Integration plugin should have correct icon");

        // Verify annotation values for TestPlugin
        org.phong.zenflow.plugin.subdomain.registry.Plugin testAnnotation =
                TestPlugin.class.getAnnotation(org.phong.zenflow.plugin.subdomain.registry.Plugin.class);

        assertEquals("test", testAnnotation.key(), "Test plugin key should be 'test'");
        assertEquals("Test Plugin", testAnnotation.name(), "Test plugin name should be 'Test Plugin'");
        assertEquals("1.0.0", testAnnotation.version(), "Test plugin version should be '1.0.0'");
        assertFalse(testAnnotation.description().isEmpty(), "Test plugin should have a description");
        assertTrue(testAnnotation.tags().length > 0, "Test plugin should have tags");
        assertEquals("ph:test-tube", testAnnotation.icon(), "Test plugin should have correct icon");
    }

    @Test
    void testPluginSynchronizerOrder() {
        // Test that PluginSynchronizer has the correct order annotation
        assertTrue(PluginSynchronizer.class.isAnnotationPresent(org.springframework.core.annotation.Order.class),
                "PluginSynchronizer should have @Order annotation");

        org.springframework.core.annotation.Order orderAnnotation =
                PluginSynchronizer.class.getAnnotation(org.springframework.core.annotation.Order.class);

        assertEquals(10, orderAnnotation.value(),
                "PluginSynchronizer should have order value of 10 to run before PluginNodeSynchronizer");
    }

    @Test
    void testPluginSynchronizerErrorHandling() {
        // Test error handling in plugin synchronization

        // Mock repository to throw an exception
        when(pluginRepository.findByKey(anyString())).thenReturn(Optional.empty());
        when(pluginRepository.save(any(Plugin.class))).thenThrow(new RuntimeException("Database error"));

        // Synchronizer should handle exceptions gracefully and continue with other plugins
        assertDoesNotThrow(() -> pluginSynchronizer.run(null),
                "Plugin synchronizer should handle individual plugin errors gracefully");

        // Verify that it attempted to save plugins despite errors
        verify(pluginRepository, atLeastOnce()).save(any(Plugin.class));
    }
}
