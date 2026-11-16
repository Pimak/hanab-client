package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.Set;

/** Strict projection of the replay leader-action union from the bot protocol. */
public sealed interface ReplayAction permits ReplayAction.Turn, ReplayAction.Arrow, ReplayAction.Sound, ReplayAction.Efficiency, ReplayAction.Hypothetical {
    String type();
    JsonNode raw();

    record Turn(int turn, JsonNode raw) implements ReplayAction { public Turn { if (turn < 0 || turn > 512 || raw == null) throw new IllegalArgumentException("invalid replay turn"); raw = raw.deepCopy(); } public Turn(int turn) { this(turn, ReplayActionFactory.turn(turn)); } public String type(){return "turn";} @Override public JsonNode raw(){return raw.deepCopy();} }
    record Arrow(Optional<JsonNode> target, JsonNode raw) implements ReplayAction { public Arrow { if (target == null || raw == null) throw new NullPointerException(); target = target.map(JsonNode::deepCopy); raw = raw.deepCopy(); } public Arrow() { this(Optional.empty(), ReplayActionFactory.arrow(null)); } public String type(){return "arrow";} @Override public Optional<JsonNode> target(){return target.map(JsonNode::deepCopy);} @Override public JsonNode raw(){return raw.deepCopy();} }
    record Sound(ReplaySoundEffect sound, JsonNode raw) implements ReplayAction { public Sound { if (sound == null || raw == null) throw new NullPointerException(); raw = raw.deepCopy(); } public Sound(ReplaySoundEffect sound) { this(sound, ReplayActionFactory.sound(sound)); } public String soundName(){ return raw.path("sound").asText(sound.name().toLowerCase(java.util.Locale.ROOT)); } public String type(){return "sound";} @Override public JsonNode raw(){return raw.deepCopy();} }
    record Efficiency(int modifier, JsonNode raw) implements ReplayAction { public Efficiency { if (modifier < -60 || modifier > 60 || raw == null) throw new IllegalArgumentException("invalid replay efficiency"); raw = raw.deepCopy(); } public Efficiency(int modifier) { this(modifier, ReplayActionFactory.efficiency(modifier)); } public String type(){return "efficiency";} @Override public JsonNode raw(){return raw.deepCopy();} }
    record Hypothetical(JsonNode control, JsonNode raw) implements ReplayAction { public Hypothetical { if (control == null || !control.isObject() || raw == null) throw new IllegalArgumentException("invalid replay hypothetical"); control = control.deepCopy(); raw = raw.deepCopy(); } public Hypothetical(JsonNode control) { this(control, ReplayActionFactory.hypothetical(control)); } public String type(){return "hypothetical";} @Override public JsonNode control(){return control.deepCopy();} @Override public JsonNode raw(){return raw.deepCopy();} }

    static ReplayAction from(JsonNode node) {
        if (node == null || !node.isObject() || !node.path("type").isTextual()) throw new IllegalArgumentException("invalid replay action");
        String type = node.path("type").textValue();
        return switch (type) {
            case "turn" -> { reject(node, Set.of("type", "turn")); yield new Turn(integer(node, "turn"), node); }
            case "arrow" -> { reject(node, Set.of("type", "target")); ReplayActionFactory.arrow(node.has("target") ? node.get("target") : null); yield new Arrow(node.has("target") ? Optional.of(node.get("target")) : Optional.empty(), node); }
            case "sound" -> { reject(node, Set.of("type", "sound")); String name=text(node, "sound"); ReplaySoundEffect effect; try { effect=ReplaySoundEffect.valueOf(name.toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException ignored) { throw new IllegalArgumentException("unknown replay sound: " + name); } yield new Sound(effect, node); }
            case "efficiency" -> { reject(node, Set.of("type", "modifier")); yield new Efficiency(integer(node, "modifier"), node); }
            case "hypothetical" -> { reject(node, Set.of("type", "control")); var control = object(node, "control"); ReplayActionFactory.hypothetical(control); yield new Hypothetical(control, node); }
            default -> throw new IllegalArgumentException("unknown replay action: " + type);
        };
    }
    private static int integer(JsonNode node, String field) { var value=node.get(field); if (value==null || !value.isIntegralNumber() || !value.canConvertToInt()) throw new IllegalArgumentException("replay action field must be an integer: "+field); return value.intValue(); }
    private static String text(JsonNode node, String field) { var value=node.get(field); if (value==null || !value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("replay action field must be text: "+field); return value.textValue(); }
    private static JsonNode object(JsonNode node, String field) { var value=node.get(field); if (value==null || !value.isObject()) throw new IllegalArgumentException("replay action field must be an object: "+field); return value; }
    private static void reject(JsonNode node, Set<String> known) { var fields=node.fieldNames(); while(fields.hasNext()) if(!known.contains(fields.next())) throw new IllegalArgumentException("unknown replay action field"); }
}
