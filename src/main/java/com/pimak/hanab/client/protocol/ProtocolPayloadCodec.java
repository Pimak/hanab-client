package com.pimak.hanab.client.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.model.Replay;
import com.pimak.hanab.client.model.ReplayDocument;
import com.pimak.hanab.client.model.User;
import java.util.*;

/** Decoder for current server payload DTOs that are not WebSocket frames. */
public final class ProtocolPayloadCodec {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ProtocolPayloadCodec() {}

    public static User decodeUser(String json) throws ProtocolDecodeException {
        JsonNode value = parse(json, "user");
        if (!value.path("userId").canConvertToLong() || value.path("userId").longValue() < 0 || !value.path("username").isTextual() || value.path("username").textValue().isBlank())
            throw invalid("invalid user payload");
        Map<String,Object> unknown = JSON.convertValue(value, new TypeReference<Map<String,Object>>() {});
        unknown.keySet().removeAll(Set.of("userId", "username"));
        return new User(value.path("userId").longValue(), value.path("username").textValue(), unknown);
    }

    public static ReplayDocument decodeReplayDocument(String json) throws ProtocolDecodeException {
        JsonNode value = parse(json, "replay");
        try {
            Map<String,Object> root = JSON.convertValue(value, new TypeReference<Map<String,Object>>() {});
            Set<String> known = Set.of("seed", "playerNames", "players", "deck", "actions", "options", "notes", "characters");
            Map<String,Object> unknown = new LinkedHashMap<>(root); unknown.keySet().removeAll(known);
            Optional<String> seed = value.path("seed").isTextual() ? Optional.of(value.path("seed").textValue()) : Optional.empty();
            List<String> names = new ArrayList<>(); JsonNode players = value.has("playerNames") ? value.get("playerNames") : value.get("players");
            if (players != null) for (JsonNode player : players) names.add(player.isObject() ? player.path("username").asText() : player.asText());
            List<Map<String,Object>> deck = maps(value.get("deck"));
            List<Map<String,Object>> actions = maps(value.get("actions"));
            Map<String,Object> options = object(value.get("options"));
            List<List<String>> notes = new ArrayList<>(); if (value.has("notes")) for (JsonNode player : value.get("notes")) { List<String> values = new ArrayList<>(); for (JsonNode note : player) values.add(note.asText()); notes.add(values); }
            return new ReplayDocument(seed, names, deck, actions, options, notes, maps(value.get("characters")), unknown);
        } catch (RuntimeException error) { throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "invalid replay payload", error); }
    }

    public static Replay decodeReplay(String json) throws ProtocolDecodeException {
        ReplayDocument document = decodeReplayDocument(json);
        Map<String,Object> unknown = new LinkedHashMap<>(document.unknownFields());
        if (!document.deck().isEmpty()) unknown.put("deck", document.deck());
        if (!document.options().isEmpty()) unknown.put("options", document.options());
        if (!document.notes().isEmpty()) unknown.put("notes", document.notes());
        if (!document.characters().isEmpty()) unknown.put("characters", document.characters());
        return new Replay(document.seed(), document.playerNames(), document.actions(), unknown);
    }

    private static JsonNode parse(String json, String name) throws ProtocolDecodeException { try { JsonNode value = JSON.readTree(Objects.requireNonNull(json, "json")); if (value == null || !value.isObject()) throw new IllegalArgumentException(name + " must be an object"); return value; } catch (Exception error) { if (error instanceof ProtocolDecodeException e) throw e; throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_JSON, "invalid " + name + " JSON", error); } }
    private static Map<String,Object> object(JsonNode value) { return value == null ? Map.of() : JSON.convertValue(value, new TypeReference<Map<String,Object>>() {}); }
    private static List<Map<String,Object>> maps(JsonNode value) { return value == null ? List.of() : JSON.convertValue(value, new TypeReference<List<Map<String,Object>>>() {}); }
    private static ProtocolDecodeException invalid(String message) { return new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, message); }
}
