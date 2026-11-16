package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** Typed assignment of a detrimental character to a game seat. */
public sealed interface CharacterAssignment permits CharacterAssignment.Plain, CharacterAssignment.Color, CharacterAssignment.Rank {
    int characterId();
    JsonNode toJson();

    record Plain(int characterId) implements CharacterAssignment {
        public Plain { checkCharacter(characterId); }
        @Override public JsonNode toJson() { return node("plain", characterId, null, null); }
    }
    record Color(int characterId, int colorIndex) implements CharacterAssignment {
        public Color { checkCharacter(characterId); if (colorIndex < 0 || colorIndex > 5) throw new IllegalArgumentException("colorIndex out of range"); }
        @Override public JsonNode toJson() { return node("color", characterId, colorIndex, null); }
    }
    record Rank(int characterId, int rank) implements CharacterAssignment {
        public Rank { checkCharacter(characterId); if (rank < 1 || rank > 5) throw new IllegalArgumentException("rank out of range"); }
        @Override public JsonNode toJson() { return node("rank", characterId, null, rank); }
    }

    static CharacterAssignment from(JsonNode value) {
        if (value == null || !value.isObject() || !value.path("type").isTextual()) throw new IllegalArgumentException("invalid character assignment");
        reject(value, Set.of("type", "characterId", "color", "rank"));
        int id = integer(value, "characterId", 0, 32_767);
        return switch (value.path("type").textValue()) {
            case "plain" -> { exact(value, Set.of("type", "characterId")); yield new Plain(id); }
            case "color" -> { exact(value, Set.of("type", "characterId", "color")); yield new Color(id, integer(value, "color", 0, 5)); }
            case "rank" -> { exact(value, Set.of("type", "characterId", "rank")); yield new Rank(id, integer(value, "rank", 1, 5)); }
            default -> throw new IllegalArgumentException("unknown character assignment type");
        };
    }
    private static JsonNode node(String type, int id, Integer color, Integer rank) {
        var n = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("type", type).put("characterId", id);
        if (color != null) n.put("color", color); if (rank != null) n.put("rank", rank); return n;
    }
    private static void checkCharacter(int value) { if (value < 0 || value > 32_767) throw new IllegalArgumentException("characterId out of range"); }
    private static int integer(JsonNode node, String field, int min, int max) { var v=node.get(field); if(v==null || !v.isIntegralNumber() || !v.canConvertToInt() || v.intValue()<min || v.intValue()>max) throw new IllegalArgumentException(field+" out of range"); return v.intValue(); }
    private static void reject(JsonNode node, Set<String> known) { var fields=node.fieldNames(); while(fields.hasNext()) if(!known.contains(fields.next())) throw new IllegalArgumentException("unknown character assignment field"); }
    private static void exact(JsonNode node, Set<String> known) { if (node.size() != known.size()) throw new IllegalArgumentException("invalid character assignment fields"); }
}
