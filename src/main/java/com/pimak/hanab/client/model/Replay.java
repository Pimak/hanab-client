package com.pimak.hanab.client.model;

import java.util.List;
import java.util.Map;

/** Imported or saved replay metadata and its opaque, ordered actions. */
public record Replay(java.util.Optional<String> seed, List<String> playerNames, List<Map<String, Object>> actions,
                     Map<String, Object> unknownFields) {
    public Replay {
        seed = java.util.Objects.requireNonNull(seed, "seed").filter(value -> !value.isBlank());
        playerNames = ModelSupport.immutableList(playerNames);
        actions = actions == null ? List.of() : actions.stream().map(ModelSupport::immutableMap).toList();
        unknownFields = ModelSupport.immutableMap(unknownFields);
    }
    public Replay(String seed, List<String> playerNames, List<Map<String, Object>> actions) {
        this(java.util.Optional.ofNullable(seed), playerNames, actions, Map.of());
    }
    public Replay(String seed, List<String> playerNames, List<Map<String, Object>> actions, Map<String, Object> unknownFields) {
        this(java.util.Optional.ofNullable(seed), playerNames, actions, unknownFields);
    }
}
