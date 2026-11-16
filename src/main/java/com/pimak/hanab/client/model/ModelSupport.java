package com.pimak.hanab.client.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.LinkedHashMap;
import com.fasterxml.jackson.databind.JsonNode;

final class ModelSupport {
    private ModelSupport() {}
    static <T> List<T> immutableList(List<T> value) {
        return List.copyOf(Objects.requireNonNull(value, "value"));
    }
    static <K, V> Map<K, V> immutableMap(Map<K, V> value) {
        var result = new LinkedHashMap<K, V>();
        Objects.requireNonNull(value, "value").forEach((key, item) -> result.put(key, (V) immutableValue(item)));
        return Collections.unmodifiableMap(result);
    }
    @SuppressWarnings("unchecked")
    static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) { var result = new LinkedHashMap<Object, Object>(); map.forEach((key, item) -> result.put(key, immutableValue(item))); return Collections.unmodifiableMap(result); }
        if (value instanceof List<?> list) return List.copyOf(list.stream().map(ModelSupport::immutableValue).toList());
        if (value instanceof java.util.Set<?> set) return java.util.Set.copyOf(set.stream().map(ModelSupport::immutableValue).toList());
        if (value instanceof JsonNode node) return node.deepCopy();
        return value;
    }
}
