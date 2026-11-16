package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/** Builders for the discriminated shared-replay action union. */
public final class ReplayActionFactory {
    private ReplayActionFactory() { }
    public static JsonNode turn(int turn) { return base("turn").put("turn", checkedTurn(turn)); }
    public static JsonNode sound(ReplaySoundEffect sound) { return base("sound").put("sound", sound.name().toLowerCase(java.util.Locale.ROOT)); }
    public static JsonNode efficiency(int modifier) { if (modifier < -60 || modifier > 60) throw new IllegalArgumentException("efficiency modifier must be between -60 and 60"); return base("efficiency").put("modifier", modifier); }
    public static JsonNode hypothetical(JsonNode control) { validateHypothetical(control); return base("hypothetical").set("control", control.deepCopy()); }
    public static JsonNode hypotheticalControl(HypotheticalControl control) { return hypothetical(java.util.Objects.requireNonNull(control, "control").toJson()); }
    public static JsonNode hypotheticalStart() { return hypothetical(control("start")); }
    public static JsonNode hypotheticalEnd() { return hypothetical(control("end")); }
    public static JsonNode hypotheticalBack() { return hypothetical(control("back")); }
    public static JsonNode showDrawnCards(boolean show) { return hypothetical(control("showDrawnCards").put("show", show)); }
    public static JsonNode hypotheticalEvent(JsonNode event) { if (event == null || !event.isObject() || !java.util.Set.of("draw","play","discard","misplay","clue","turn","morph","unmorph").contains(event.path("type").asText())) throw new IllegalArgumentException("invalid hypothetical event"); return hypothetical(control("event").set("event", event.deepCopy())); }
    public static JsonNode arrow(JsonNode target) { if (target != null) validateArrowTarget(target); var result = base("arrow"); if (target != null) result.set("target", target.deepCopy()); return result; }
    private static ObjectNode base(String type) { return JsonNodeFactory.instance.objectNode().put("type", type); }
    private static ObjectNode control(String type) { return base(type); }
    private static int checkedTurn(int turn) { if (turn < 0 || turn > 512) throw new IllegalArgumentException("turn must be between 0 and 512"); return turn; }
    private static void validateHypothetical(JsonNode control) {
        if (control == null || !control.isObject() || !control.path("type").isTextual()) throw new IllegalArgumentException("invalid hypothetical control");
        switch (control.path("type").textValue()) {
            case "start", "end", "back" -> reject(control, "type");
            case "showDrawnCards" -> { reject(control, "type", "show"); if (!control.path("show").isBoolean()) throw new IllegalArgumentException("showDrawnCards requires show"); }
            case "event" -> { reject(control, "type", "event"); if (!control.path("event").isObject()) throw new IllegalArgumentException("event control requires event"); validateHypotheticalEvent(control.path("event")); }
            default -> throw new IllegalArgumentException("invalid hypothetical control");
        }
    }
    private static void validateHypotheticalEvent(JsonNode event) {
        if (!event.path("type").isTextual()) throw new IllegalArgumentException("hypothetical event requires type");
        switch (event.path("type").textValue()) {
            case "draw", "play", "discard", "misplay" -> { reject(event, "type", "playerIndex", "order", "identity"); index(event, "playerIndex", 0, 5); order(event, "order"); if (event.has("identity")) identity(event.path("identity"), false); }
            case "clue" -> { reject(event, "type", "clue", "playerIndex", "receiverIndex", "orders", "ignoreNegative"); index(event, "playerIndex", 0, 5); index(event, "receiverIndex", 0, 5); if (!event.path("orders").isArray() || event.path("orders").size() > 6) throw new IllegalArgumentException("invalid clue orders"); event.path("orders").forEach(value -> { if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0 || value.intValue() > 65) throw new IllegalArgumentException("invalid clue order"); }); if (event.has("ignoreNegative") && !event.path("ignoreNegative").isBoolean()) throw new IllegalArgumentException("ignoreNegative must be boolean"); if (event.has("clue")) clue(event.path("clue")); }
            case "turn" -> { reject(event, "type", "turn", "currentPlayerIndex"); if (!event.path("turn").isIntegralNumber() || !event.path("turn").canConvertToInt() || event.path("turn").intValue() < 0 || event.path("turn").intValue() > 512) throw new IllegalArgumentException("invalid hypothetical turn"); if (event.has("currentPlayerIndex")) index(event, "currentPlayerIndex", 0, 5); }
            case "morph" -> { reject(event, "type", "order", "identity"); order(event, "order"); if (!event.path("identity").isObject()) throw new IllegalArgumentException("morph requires identity"); identity(event.path("identity"), true); }
            case "unmorph" -> { reject(event, "type", "order"); order(event, "order"); }
            default -> throw new IllegalArgumentException("invalid hypothetical event");
        }
    }
    private static void validateArrowTarget(JsonNode target) { if (!target.isObject() || !target.path("type").isTextual() || !java.util.Set.of("card","board").contains(target.path("type").asText())) throw new IllegalArgumentException("invalid arrow target"); if (target.path("type").asText().equals("card")) { reject(target, "type", "order"); order(target, "order"); } if (target.path("type").asText().equals("board")) { reject(target, "type", "element"); if (!java.util.Set.of("deck","turn","score","maxScore","clues","strike1","strike2","strike3","pace","efficiency","minEfficiency").contains(target.path("element").asText())) throw new IllegalArgumentException("invalid board arrow element"); } }
    private static void reject(JsonNode object, String... known) { var allowed = java.util.Set.of(known); var fields = object.fieldNames(); while (fields.hasNext()) if (!allowed.contains(fields.next())) throw new IllegalArgumentException("unknown replay action field"); }
    private static void order(JsonNode object, String field) { if (!object.path(field).isIntegralNumber() || !object.path(field).canConvertToInt() || object.path(field).intValue() < 0 || object.path(field).intValue() > 65) throw new IllegalArgumentException("invalid card order"); }
    private static void index(JsonNode object, String field, int min, int max) { if (!object.path(field).isIntegralNumber() || !object.path(field).canConvertToInt() || object.path(field).intValue() < min || object.path(field).intValue() > max) throw new IllegalArgumentException("invalid player index"); }
    private static void identity(JsonNode value, boolean morph) { if (!value.isObject()) throw new IllegalArgumentException("identity must be an object"); reject(value, "suitIndex", "rank"); if (!value.has("suitIndex") && !value.has("rank") && !morph) throw new IllegalArgumentException("card identity is empty"); if (value.has("suitIndex")) index(value, "suitIndex", 0, 5); if (value.has("rank") && (!value.path("rank").isIntegralNumber() || !value.path("rank").canConvertToInt() || value.path("rank").intValue() < 1 || value.path("rank").intValue() > 5)) throw new IllegalArgumentException("invalid rank"); }
    private static void clue(JsonNode value) { if (!value.isObject()) throw new IllegalArgumentException("clue must be an object"); reject(value, "type", "color", "rank"); String type=value.path("type").asText(); if (!type.equals("color") && !type.equals("rank")) throw new IllegalArgumentException("invalid clue type"); if (type.equals("color")) index(value, "color", 0, 5); else if (!value.path("rank").isIntegralNumber() || !value.path("rank").canConvertToInt() || value.path("rank").intValue() < 1 || value.path("rank").intValue() > 5) throw new IllegalArgumentException("invalid clue rank"); }
}
