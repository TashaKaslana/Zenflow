package org.phong.zenflow.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
public class LoadSchemaHelper {
    public static Map<String, Object> loadSchema(Class<?> clazz, String customPath, String defaultName) {
        String resourcePath = extractPath(clazz, customPath, defaultName);
        String classpathResource = "/" + resourcePath;

        // 1. Try classpath first (works for packaged nodes)
        InputStream classpathStream = clazz.getResourceAsStream(classpathResource);
        if (classpathStream != null) {
            try (InputStream is = classpathStream) {
                return ObjectConversion.getObjectMapper()
                        .readValue(is, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load schema from classpath for " + clazz.getName(), e);
            }
        }

        // 2. Fallback to file system (works for external plugin nodes)
        Path filePath = Paths.get(resourcePath);
        if (Files.exists(filePath)) {
            try (InputStream is = Files.newInputStream(filePath)) {
                return ObjectConversion.getObjectMapper()
                        .readValue(is, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load schema from filesystem for " + clazz.getName(), e);
            }
        }

        log.warn("No {} found for {}", defaultName, clazz.getName());
        return Map.of();
    }

    public static String extractPath(Class<?> clazz, String customPath, String defaultName) {
        Path basePath = Paths.get(clazz.getPackageName().replace('.', '/'));

        Path resultPath;
        if (customPath.isEmpty()) {
            resultPath = basePath.resolve(defaultName);
        } else if (customPath.startsWith("/")) {
            resultPath = basePath.resolve(customPath.substring(1));
        } else if (customPath.startsWith("./")) {
            resultPath = basePath.resolve(customPath.substring(2));
        } else if (customPath.startsWith("../")) {
            resultPath = basePath.resolve(customPath).normalize();
        } else {
            resultPath = basePath.resolve(customPath);
        }

        return resultPath.toString().replace("\\", "/");
    }
}
