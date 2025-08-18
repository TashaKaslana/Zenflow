package org.phong.zenflow.plugin.subdomain.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.infrastructure.persistence.repository.PluginRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * Scans the classpath for {@link org.phong.zenflow.plugin.subdomain.registry.Plugin}
 * annotations and synchronizes their metadata with the {@code plugins} table.
 * <p>
 * This synchronizer runs before the PluginNodeSynchronizer to ensure plugins are registered first.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10) // Run before PluginNodeSynchronizer (order 20)
public class PluginSynchronizer implements ApplicationRunner {

    private final PluginRepository pluginRepository;

    @Override
    public void run(ApplicationArguments args) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                org.phong.zenflow.plugin.subdomain.registry.Plugin.class));

        scanner.findCandidateComponents("org.phong.zenflow")
                .forEach(beanDefinition -> handleClass(beanDefinition.getBeanClassName()));
    }

    private void handleClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            org.phong.zenflow.plugin.subdomain.registry.Plugin annotation =
                    clazz.getAnnotation(
                            org.phong.zenflow.plugin.subdomain.registry.Plugin.class);
            if (annotation == null) {
                return;
            }

            Plugin entity = pluginRepository.findByKey(annotation.key())
                    .orElseGet(Plugin::new);

            entity.setKey(annotation.key());
            entity.setName(annotation.name());
            entity.setVersion(annotation.version());
            entity.setDescription(annotation.description());
            entity.setTags(Arrays.asList(annotation.tags()));
            entity.setIcon(annotation.icon());
            entity.setRegistryUrl(annotation.registryUrl().isEmpty() ? null : annotation.registryUrl());
            entity.setVerified(annotation.verified());
            entity.setPublisherId(UUID.fromString(annotation.publisherId()));

            pluginRepository.save(entity);
            log.info("Synchronized plugin: {} v{}", annotation.key(), annotation.version());
        } catch (Exception e) {
            log.error("Failed to synchronize plugin for class {}", className, e);
        }
    }
}
