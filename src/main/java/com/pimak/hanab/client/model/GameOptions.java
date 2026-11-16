package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.Optional;

/** Typed projection of the official table game options. */
public record GameOptions(int variantId, Optional<TimeControl> timeControl,
                          boolean speedrun, boolean deckPlays, boolean emptyClues,
                          boolean oneExtraCard, boolean oneLessCard,
                          boolean allOrNothing, boolean detrimentalCharacters) {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> FIELDS = Set.of("variantId", "timeControl", "speedrun", "deckPlays", "emptyClues", "oneExtraCard", "oneLessCard", "allOrNothing", "detrimentalCharacters");
    public GameOptions {
        if (variantId < 0 || variantId > 32_767) throw new IllegalArgumentException("variantId is out of range");
        timeControl = timeControl == null ? Optional.empty() : timeControl;
    }
    public static GameOptions defaults() { return new GameOptions(0, Optional.empty(), false, false, false, false, false, false, false); }
    public static GameOptions fromJson(JsonNode value) {
        if (value == null || !value.isObject()) throw new IllegalArgumentException("game options must be an object");
        var fields = value.fieldNames(); while (fields.hasNext()) if (!FIELDS.contains(fields.next())) throw new IllegalArgumentException("unknown game option");
        return new GameOptions(integer(value, "variantId", 0, 32_767), timeControl(value.get("timeControl")), bool(value, "speedrun"), bool(value, "deckPlays"), bool(value, "emptyClues"), bool(value, "oneExtraCard"), bool(value, "oneLessCard"), bool(value, "allOrNothing"), bool(value, "detrimentalCharacters"));
    }
    public JsonNode toJson() {
        ObjectNode result = JSON.createObjectNode().put("variantId", variantId).put("speedrun", speedrun).put("deckPlays", deckPlays).put("emptyClues", emptyClues).put("oneExtraCard", oneExtraCard).put("oneLessCard", oneLessCard).put("allOrNothing", allOrNothing).put("detrimentalCharacters", detrimentalCharacters);
        timeControl.ifPresent(value -> result.set("timeControl", JSON.createObjectNode().put("baseSeconds", value.baseSeconds()).put("perTurnSeconds", value.perTurnSeconds())));
        return result;
    }
    private static Optional<TimeControl> timeControl(JsonNode value) {
        if (value == null) return Optional.empty();
        if (value.isNull()) return Optional.empty();
        if (!value.isObject()) throw new IllegalArgumentException("timeControl must be an object");
        rejectUnknown(value, Set.of("baseSeconds", "perTurnSeconds"));
        if (!value.has("baseSeconds") || !value.has("perTurnSeconds")) throw new IllegalArgumentException("timeControl requires baseSeconds and perTurnSeconds");
        return Optional.of(new TimeControl(integer(value, "baseSeconds", 0, 604_800), integer(value, "perTurnSeconds", 0, 86_400)));
    }
    private static void rejectUnknown(JsonNode node, Set<String> known) { var fields = node.fieldNames(); while (fields.hasNext()) if (!known.contains(fields.next())) throw new IllegalArgumentException("unknown time control field"); }
    private static int integer(JsonNode node, String name, int min, int max) { var value=node.get(name); if(value==null) return min; if(!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue()<min || value.intValue()>max) throw new IllegalArgumentException(name+" is out of range"); return value.intValue(); }
    private static boolean bool(JsonNode node, String name) { var value=node.get(name); if(value==null) return false; if(!value.isBoolean()) throw new IllegalArgumentException(name+" must be boolean"); return value.booleanValue(); }
}
