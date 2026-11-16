package com.pimak.hanab.client.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lossless replay import/export model matching Hanab Live's optional replay fields. */
public record ReplayDocument(Optional<String> seed, List<String> playerNames, List<Map<String, Object>> deck,
                             List<Map<String, Object>> actions, Map<String, Object> options,
                             List<List<String>> notes, List<Map<String, Object>> characters,
                             Map<String, Object> unknownFields) {
    public ReplayDocument {
        seed = java.util.Objects.requireNonNull(seed, "seed").filter(value -> !value.isBlank());
        playerNames = List.copyOf(playerNames == null ? List.of() : playerNames);
        deck = immutableMaps(deck);
        actions = immutableMaps(actions);
        options = ModelSupport.immutableMap(options);
        notes = notes == null ? List.of() : notes.stream().map(values -> List.copyOf(values == null ? List.of() : values)).toList();
        characters = immutableMaps(characters);
        unknownFields = ModelSupport.immutableMap(unknownFields);
    }

    private static List<Map<String, Object>> immutableMaps(List<Map<String, Object>> values) {
        return values == null ? List.of() : values.stream().map(ModelSupport::immutableMap).toList();
    }
}
