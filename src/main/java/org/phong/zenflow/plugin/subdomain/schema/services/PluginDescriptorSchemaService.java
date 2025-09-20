package org.phong.zenflow.plugin.subdomain.schema.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.phong.zenflow.plugin.exception.PluginException;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaMissingException;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PluginDescriptorSchemaService {

    private static final String DESCRIPTOR_CACHE_KEY_FORMAT = "%s:%s:%s";
    private static final String PROFILE_SECTION = "profile";
    private static final String SETTING_SECTION = "setting";

    private final PluginService pluginService;
    private final SchemaIndexRegistry schemaIndexRegistry;
    private final Cache<String, JSONObject> profileCache;
    private final Cache<String, JSONObject> settingCache;
    private final boolean useFileBasedLoading;

    public PluginDescriptorSchemaService(
            PluginService pluginService,
            SchemaIndexRegistry schemaIndexRegistry,
            @Value("") long cacheTtlSeconds,
            @Value("") boolean useFileBasedLoading) {
        this.pluginService = pluginService;
        this.schemaIndexRegistry = schemaIndexRegistry;
        this.useFileBasedLoading = useFileBasedLoading;
        var ttl = java.time.Duration.ofSeconds(cacheTtlSeconds);
        this.profileCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
        this.settingCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
    }

    public JSONObject getProfileDescriptorSchema(UUID pluginId, String pluginKey, String descriptorId) {
        return getDescriptorSchema(pluginId, pluginKey, descriptorId, PROFILE_SECTION, profileCache);
    }

    public JSONObject getSettingDescriptorSchema(UUID pluginId, String pluginKey, String descriptorId) {
        return getDescriptorSchema(pluginId, pluginKey, descriptorId, SETTING_SECTION, settingCache);
    }

    private JSONObject getDescriptorSchema(UUID pluginId,
                                           String pluginKey,
                                           String descriptorId,
                                           String section,
                                           Cache<String, JSONObject> cache) {
        if (descriptorId == null || descriptorId.isBlank()) {
            throw new PluginException("Descriptor id must be provided");
        }

        List<String> bases = new ArrayList<>();
        if (pluginId != null) {
            bases.add(pluginId.toString());
        }
        if (pluginKey != null && !pluginKey.isBlank()) {
            bases.add(pluginKey);
        }
        if (bases.isEmpty()) {
            throw new PluginException("Plugin identifier is required to resolve descriptor schema");
        }

        for (String base : bases) {
            JSONObject cached = cache.getIfPresent(buildDescriptorCacheKey(base, section, descriptorId));
            if (cached != null) {
                return cached;
            }
        }

        SchemaIndexRegistry.SchemaLocation location = findDescriptorLocation(
                pluginId, pluginKey, descriptorId, section
        );
        if (useFileBasedLoading && location != null) {
            JSONObject loaded = loadSchemaFromLocation(location);
            if (loaded != null) {
                cacheLoadedDescriptors(cache, bases, section, descriptorId, loaded);
                return loaded;
            }
        }

        Map<String, Object> pluginSchema = null;
        if (pluginId != null) {
            pluginSchema = pluginService.findPluginById(pluginId).getPluginSchema();
        } else {
            pluginSchema = pluginService.getPluginSchemaByKey(pluginKey);
        }

        Map<String, Object> descriptorSchema = extractDescriptorSchemaFromPluginMap(
                pluginSchema, section, descriptorId
        );
        if (descriptorSchema != null && !descriptorSchema.isEmpty()) {
            JSONObject json = new JSONObject(descriptorSchema);
            cacheLoadedDescriptors(cache, bases, section, descriptorId, json);
            return json;
        }

        throw new NodeSchemaMissingException("No schema found for descriptor", List.of(descriptorId));
    }

    private void cacheLoadedDescriptors(Cache<String, JSONObject> cache,
                                        List<String> bases,
                                        String section,
                                        String descriptorId,
                                        JSONObject json) {
        bases.forEach(base -> cache.put(buildDescriptorCacheKey(base, section, descriptorId), json));
    }

    private SchemaIndexRegistry.SchemaLocation findDescriptorLocation(UUID pluginId,
                                                                      String pluginKey,
                                                                      String descriptorId,
                                                                      String section) {
        SchemaIndexRegistry.SchemaLocation location = null;
        if (PROFILE_SECTION.equals(section)) {
            if (pluginId != null) {
                location = schemaIndexRegistry.getProfileSchemaLocation(pluginId, descriptorId);
            }
            if (location == null && pluginKey != null) {
                location = schemaIndexRegistry.getProfileSchemaLocation(pluginKey, descriptorId);
            }
        } else {
            if (pluginId != null) {
                location = schemaIndexRegistry.getSettingSchemaLocation(pluginId, descriptorId);
            }
            if (location == null && pluginKey != null) {
                location = schemaIndexRegistry.getSettingSchemaLocation(pluginKey, descriptorId);
            }
        }
        return location;
    }

    private Map<String, Object> extractDescriptorSchemaFromPluginMap(Map<String, Object> pluginSchema,
                                                                     String section, String descriptorId) {
        if (pluginSchema == null || pluginSchema.isEmpty()) {
            return null;
        }
        Object sections = pluginSchema.get(section.equals(PROFILE_SECTION) ? "profiles" : "settings");
        if (!(sections instanceof List<?> list)) {
            return null;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object idVal = map.get("id");
                if (descriptorId.equals(idVal)) {
                    Object schema = map.get("schema");
                    if (schema instanceof Map<?, ?> schemaMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cast = (Map<String, Object>) schemaMap;
                        return cast;
                    }
                }
            }
        }
        return null;
    }

    private JSONObject loadSchemaFromLocation(SchemaIndexRegistry.SchemaLocation location) {
        if (location == null) {
            return null;
        }
        try {
            Map<String, Object> schema = LoadSchemaHelper.loadSchema(location.clazz(), location.schemaPath(), "schema.json");
            if (schema.isEmpty()) {
                return null;
            }
            return new JSONObject(schema);
        } catch (Exception ex) {
            log.warn("Failed to load descriptor schema from {}: {}", location.schemaPath(), ex.getMessage());
            return null;
        }
    }

    private String buildDescriptorCacheKey(String base, String section, String descriptorId) {
        return DESCRIPTOR_CACHE_KEY_FORMAT.formatted(base, section, descriptorId);
    }
}
