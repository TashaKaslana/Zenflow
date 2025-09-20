package org.phong.zenflow.core.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.phong.zenflow.core.annotations.ExcludeFromPayload;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ObjectConversion {

    private static final ObjectMapper mapper;
    private static final Map<Class<?>, Set<String>> excludedFieldsCache = new ConcurrentHashMap<>();

    static {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.addMixIn(org.springframework.context.ApplicationEvent.class, IgnoreSourceMixin.class);
    }

    private ObjectConversion() {}

    public static Map<String, Object> convertObjectToMap(Object obj) {
        return mapper.convertValue(obj, new TypeReference<>() {});
    }

    public static ObjectMapper getObjectMapper() {
        return mapper;
    }

    public static <T> Map<String, T> convertObjectToMap(Object obj, Class<T> valueType) {
        JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, String.class, valueType);
        return mapper.convertValue(obj, mapType);
    }

    public static List<Object> convertObjectToList(Object obj) {
        return mapper.convertValue(obj, new TypeReference<>() {});
    }

    public static <T> List<T> convertObjectToList(Object obj, Class<T> elementType) {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return mapper.convertValue(obj, type);
    }

    public static Map<String, Object> convertObjectToFilteredMap(Object obj) {
        return convertObjectToFilteredMap(obj, null);
    }

    public static Map<String, Object> convertObjectToFilteredMap(Object obj, Set<String> includedFields) {
        Map<String, Object> fullMap = convertObjectToMap(obj);
        Set<String> excludedFields = getExcludedFieldNames(obj.getClass());

        return fullMap.entrySet().stream()
                .filter(entry -> !excludedFields.contains(entry.getKey()))
                .filter(entry -> includedFields == null || includedFields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, Object> convertObjectToPartialMap(Object obj, String... includedFields) {
        return convertObjectToFilteredMap(obj, Set.of(includedFields));
    }

    private static Set<String> getExcludedFieldNames(Class<?> clazz) {
        return excludedFieldsCache.computeIfAbsent(clazz, ObjectConversion::findExcludedFields);
    }

    private static Set<String> findExcludedFields(Class<?> clazz) {
        Set<String> result = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ExcludeFromPayload.class)) {
                result.add(field.getName());
            }
        }
        return result;
    }

    public abstract static class IgnoreSourceMixin {
        @JsonIgnore
        abstract Object getSource();
    }

    public static <T> T safeConvert(Object input, Class<T> targetType) {
        if (targetType.isInstance(input)) return targetType.cast(input);
        return mapper.convertValue(input, targetType);
    }

    public static <T> T safeConvert(Object input, TypeReference<T> typeReference) {
        if (input instanceof String) {
            try {
                return mapper.readValue((String) input, typeReference);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to convert String to type: " + typeReference.getType(), e);
            }
        }
        return mapper.convertValue(input, typeReference);
    }

    public static <T> T deepCopy(T object, Class<T> targetType) {
        try {
            String json = mapper.writeValueAsString(object);
            return mapper.readValue(json, targetType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create deep copy of object", e);
        }
    }
}
