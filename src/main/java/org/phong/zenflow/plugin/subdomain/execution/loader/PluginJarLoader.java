package org.phong.zenflow.plugin.subdomain.execution.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.annotation.PluginNode;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNodeDefinitionRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Loads external plugin node executors packaged in JAR files. Classes annotated with
 * {@link PluginNode} are instantiated and registered with the {@link PluginNodeExecutorRegistry}.
 * Metadata and configuration schemas are stored in the {@link PluginNodeDefinitionRegistry}.
 */
@Component
@Slf4j
public class PluginJarLoader implements ApplicationRunner {

    private final PluginNodeExecutorRegistry executorRegistry;
    private final PluginNodeDefinitionRegistry definitionRegistry;
    private final Environment environment;
    private final List<String> jarPaths = new CopyOnWriteArrayList<>();
    private final List<ClassLoader> classLoaders = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public PluginJarLoader(PluginNodeExecutorRegistry executorRegistry,
                           PluginNodeDefinitionRegistry definitionRegistry,
                           Environment environment) {
        this.executorRegistry = executorRegistry;
        this.definitionRegistry = definitionRegistry;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] configured = environment.getProperty("plugin.jar-paths", String[].class, new String[]{});
        for (String path : configured) {
            if (path != null && !path.isBlank()) {
                jarPaths.add(path);
                loadJar(path);
            }
        }
    }

    /**
     * Register a new JAR at runtime via API.
     */
    public void registerJarPath(String jarPath) {
        jarPaths.add(jarPath);
        loadJar(jarPath);
    }

    private void loadJar(String jarPath) {
        Path path = Path.of(jarPath);
        if (!Files.exists(path)) {
            log.warn("Plugin JAR does not exist: {}", jarPath);
            return;
        }
        try {
            URLClassLoader cl = new URLClassLoader(new URL[]{path.toUri().toURL()}, getClass().getClassLoader());
            classLoaders.add(cl); // keep a reference so classes remain loadable
            try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(path))) {
                JarEntry entry;
                while ((entry = jarStream.getNextJarEntry()) != null) {
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    Class<?> raw;
                    try {
                        raw = Class.forName(className, true, cl);
                    } catch (Throwable ex) {
                        log.debug("Skipping class {} due to load error", className, ex);
                        continue;
                    }
                    if (!PluginNodeExecutor.class.isAssignableFrom(raw)) {
                        continue;
                    }
                    PluginNode annotation = raw.getAnnotation(PluginNode.class);
                    if (annotation == null) {
                        continue;
                    }
                    registerExecutor(raw, annotation, cl);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load plugin jar {}", jarPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerExecutor(Class<?> raw, PluginNode annotation, ClassLoader cl)
            throws ReflectiveOperationException, IOException {
        PluginNodeExecutor executor = ((Class<? extends PluginNodeExecutor>) raw)
                .getDeclaredConstructor().newInstance();
        executorRegistry.register(executor);

        Map<String, Object> schema = loadSchema(cl, annotation.schema());
        PluginNodeIdentifier identifier = new PluginNodeIdentifier(
                annotation.plugin(),
                annotation.name(),
                annotation.version(),
                annotation.executorType()
        );
        definitionRegistry.upsert(identifier, schema);
        log.info("Registered plugin node {}:{}:{}", annotation.plugin(), annotation.name(), annotation.version());
    }

    private Map<String, Object> loadSchema(ClassLoader cl, String schemaPath) throws IOException {
        if (schemaPath == null || schemaPath.isBlank()) {
            return Collections.emptyMap();
        }
        try (InputStream in = cl.getResourceAsStream(schemaPath)) {
            if (Objects.isNull(in)) {
                log.warn("Schema resource {} not found", schemaPath);
                return Collections.emptyMap();
            }
            return mapper.readValue(in, new TypeReference<Map<String, Object>>() {});
        }
    }
}
