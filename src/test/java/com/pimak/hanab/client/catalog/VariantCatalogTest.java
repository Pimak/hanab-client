package com.pimak.hanab.client.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class VariantCatalogTest {
    @Test
    void decodesTheTwoProtocolPayloadsAndKeepsThemImmutable() {
        VariantCatalog catalog = VariantCatalog.fromJsonObjects(
                Map.of("1", "No Variant"),
                Map.of("1", Map.of("name", "No Variant", "suits", List.of("Red", "Blue"),
                        "stackSize", 10, "maxScore", 20, "future", "kept")));

        assertEquals("No Variant", catalog.name("1").orElseThrow());
        assertEquals(List.of("Red", "Blue"), catalog.variant("1").orElseThrow().suits());
        assertEquals("kept", catalog.variant("1").orElseThrow().unknownFields().get("future"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.names().put("2", "Other"));
        assertThrows(UnsupportedOperationException.class, () -> catalog.variant("1").orElseThrow().suits().add("Green"));
    }

    @Test
    void loadsBothEndpointsThroughTheSuppliedFunction() {
        AtomicReference<List<String>> paths = new AtomicReference<>(List.of());
        Function<String, CompletionStage<Map<String, ?>>> get = path -> {
            paths.updateAndGet(old -> { var next = new java.util.ArrayList<>(old); next.add(path); return next; });
            if (path.endsWith("variants")) return CompletableFuture.completedFuture(Map.of("7", "Example"));
            return CompletableFuture.completedFuture(Map.of("7", Map.of("name", "Example", "suits", List.of("Red"), "stackSize", 5, "maxScore", 5)));
        };

        VariantCatalog catalog = VariantCatalog.load(get).join();

        assertEquals(List.of("/api/v1/variants", "/api/v1/variants-full"), paths.get());
        assertEquals(5, catalog.variant("7").orElseThrow().stackSize());
    }

    @Test
    void genericValueObjectsCopyMutableInputs() {
        var values = new java.util.ArrayList<>(List.of("a"));
        Pagination<String> page = new Pagination<>(values, 0, 2);
        values.add("b");
        assertEquals(List.of("a"), page.values());
        assertTrue(page.hasNext());
        assertEquals("value", new Tag<>("example", "value").value());
        assertEquals("https://example.test", new ShareLink<>(java.net.URI.create("https://example.test"), "value").uri().toString());
    }

    @Test
    void rejectsFractionalNumbersAndDeepCopiesUnknownFields() {
        assertThrows(IllegalArgumentException.class, () -> VariantCatalog.fromJsonObjects(
                Map.of("1", "Example"), Map.of("1", Map.of("name", "Example", "suits", List.of("Red"),
                        "stackSize", 3.5, "maxScore", 3))));
        var nested = new java.util.ArrayList<>(List.of("a"));
        var variant = new VariantCatalog.Variant("1", "Example", List.of("Red"), 3, 3, Map.of("nested", nested));
        nested.add("b");
        assertEquals(List.of("a"), variant.unknownFields().get("nested"));
        var stored = (List<?>) variant.unknownFields().get("nested");
        assertThrows(UnsupportedOperationException.class, () -> {
            @SuppressWarnings("unchecked") var mutableView = (List<Object>) stored;
            mutableView.add("c");
        });
        var withNull = new VariantCatalog.Variant("2", "Future", List.of("Red"), 1, 1,
                Map.of("future", java.util.Arrays.asList("known", null)));
        assertEquals(2, ((List<?>) withNull.unknownFields().get("future")).size());
    }
}
