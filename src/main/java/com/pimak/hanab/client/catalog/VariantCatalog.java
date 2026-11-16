package com.pimak.hanab.client.catalog;

import com.pimak.hanab.client.transport.HttpJsonTransport;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/** Immutable names and metadata published by the Hanab variant catalogue. */
public final class VariantCatalog {
    private final Map<String, String> names;
    private final Map<String, Variant> variants;

    public VariantCatalog(Map<String, String> names, Map<String, Variant> variants) {
        this.names = immutableMap(names);
        this.variants = immutableMap(variants);
    }

    public Map<String, String> names() { return names; }
    public Map<String, Variant> variants() { return variants; }
    public Optional<Variant> variant(String id) { return Optional.ofNullable(variants.get(Objects.requireNonNull(id, "id"))); }
    public Optional<String> name(String id) { return Optional.ofNullable(names.get(Objects.requireNonNull(id, "id"))); }

    /** Loads both transparent response objects from the versioned public API. */
    public static CompletableFuture<VariantCatalog> load(HttpJsonTransport transport) {
        Objects.requireNonNull(transport, "transport");
        return load(transport::get);
    }

    /** Loads both endpoints through a caller-provided asynchronous JSON function. */
    public static CompletableFuture<VariantCatalog> load(
            Function<String, ? extends CompletionStage<? extends Map<String, ?>>> getJson) {
        Objects.requireNonNull(getJson, "getJson");
        try {
            CompletionStage<? extends Map<String, ?>> names = Objects.requireNonNull(getJson.apply("/api/v1/variants"), "names stage");
            CompletionStage<? extends Map<String, ?>> full = Objects.requireNonNull(getJson.apply("/api/v1/variants-full"), "full stage");
            return names.thenCombine(full, VariantCatalog::fromJsonObjects).toCompletableFuture();
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    /** Builds a catalogue from the decoded responses of the two protocol endpoints. */
    public static VariantCatalog fromJsonObjects(Map<String, ?> namesPayload, Map<String, ?> fullPayload) {
        Objects.requireNonNull(namesPayload, "namesPayload");
        Objects.requireNonNull(fullPayload, "fullPayload");
        Map<String, String> names = new LinkedHashMap<>();
        namesPayload.forEach((id, value) -> names.put(requiredText(id, "variant id"), requiredText(value, "variant name")));
        Map<String, Variant> variants = new LinkedHashMap<>();
        fullPayload.forEach((id, value) -> variants.put(requiredText(id, "variant id"), parseVariant(requiredText(id, "variant id"), value)));
        for (Map.Entry<String, Variant> entry : variants.entrySet()) names.putIfAbsent(entry.getKey(), entry.getValue().name());
        return new VariantCatalog(names, variants);
    }

    private static Variant parseVariant(String id, Object raw) {
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("variant " + id + " must be an object");
        String name = requiredText(map.get("name"), "variant name");
        Object rawSuits = map.get("suits");
        if (!(rawSuits instanceof List<?> suits)) throw new IllegalArgumentException("variant " + id + " suits must be a list");
        List<String> suitNames = suits.stream().map(suit -> requiredText(suit, "suit name")).toList();
        if (suitNames.isEmpty()) throw new IllegalArgumentException("variant " + id + " must contain at least one suit");
        int stackSize = number(map.get("stackSize"), "stackSize", id);
        int maxScore = number(map.get("maxScore"), "maxScore", id);
        Map<String, Object> unknown = new LinkedHashMap<>();
        map.forEach((key, value) -> { if (!Set.of("name", "suits", "stackSize", "maxScore").contains(String.valueOf(key))) unknown.put(String.valueOf(key), immutableValue(value)); });
        return new Variant(id, name, suitNames, stackSize, maxScore, unknown);
    }

    private static int number(Object value, String field, String id) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException("variant " + id + " " + field + " must be numeric");
        try {
            java.math.BigDecimal decimal;
            if (number instanceof java.math.BigDecimal valueDecimal) decimal = valueDecimal;
            else if (number instanceof java.math.BigInteger valueInteger) decimal = new java.math.BigDecimal(valueInteger);
            else if (number instanceof Double valueDouble) { if (!Double.isFinite(valueDouble)) throw new ArithmeticException(); decimal = java.math.BigDecimal.valueOf(valueDouble); }
            else if (number instanceof Float valueFloat) { if (!Float.isFinite(valueFloat)) throw new ArithmeticException(); decimal = java.math.BigDecimal.valueOf(valueFloat.doubleValue()); }
            else decimal = new java.math.BigDecimal(number.toString());
            return decimal.intValueExact();
        } catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException("variant " + id + " " + field + " must be an integer in int range", e); }
    }

    private static String requiredText(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return text;
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(value, "value")));
    }

    private static Map<String, Object> immutableObjectMap(Map<String, ?> value) {
        Map<String, Object> copy = new LinkedHashMap<>();
        Objects.requireNonNull(value, "value").forEach((key, item) -> copy.put(key, immutableValue(item)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), immutableValue(item)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) return Collections.unmodifiableList(list.stream().map(VariantCatalog::immutableValue).toList());
        if (value instanceof Set<?> set) {
            Set<Object> copy = new LinkedHashSet<>();
            set.forEach(item -> copy.add(immutableValue(item)));
            return Collections.unmodifiableSet(copy);
        }
        return value;
    }

    /** Metadata returned by `/api/v1/variants-full`. */
    public record Variant(String id, String name, List<String> suits, int stackSize, int maxScore,
                          Map<String, Object> unknownFields) {
        public Variant {
            if (id == null || id.isBlank() || name == null || name.isBlank()) throw new IllegalArgumentException("variant id and name must not be blank");
            suits = List.copyOf(Objects.requireNonNull(suits, "suits"));
            unknownFields = immutableObjectMap(unknownFields == null ? Map.of() : unknownFields);
            if (stackSize < 0 || maxScore < 0) throw new IllegalArgumentException("variant values must be non-negative");
        }
        public Variant(String id, String name, List<String> suits, int stackSize, int maxScore) {
            this(id, name, suits, stackSize, maxScore, Map.of());
        }
    }
}
