package com.pimak.hanab.client.protocol;

import com.pimak.hanab.client.model.GameState;
import com.pimak.hanab.client.model.Replay;
import com.pimak.hanab.client.model.ReplayDocument;
import com.pimak.hanab.client.model.Session;
import com.pimak.hanab.client.model.Table;
import com.pimak.hanab.client.model.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JDK-only JSON codec for the stable protocol seam. */
public final class ProtocolCodec {
    private ProtocolCodec() {}

    public static String encode(OutboundCommand command) { return encode(CommandEnvelope.of(command)); }
    public static String encode(CommandEnvelope envelope) {
        return envelope.command() + " " + Json.write(envelope.payload());
    }

    public static CommandEnvelope decode(String json) throws ProtocolDecodeException {
        if (json == null || json.isBlank()) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_JSON, "empty wire frame");
        int separator = json.indexOf(' ');
        var command = separator < 0 ? json : json.substring(0, separator);
        var payload = object(Json.parse(separator < 0 ? "{}" : json.substring(separator + 1).trim()), "payload");
        if (command.equals("chat")) { rejectUnknown(payload, Map.of("text", true, "tableId", true), command); string(payload, "text"); }
        else if (command.equals("tableJoin")) { rejectUnknown(payload, Map.of("tableId", true, "password", true), command); number(payload, "tableId"); if (payload.containsKey("password")) nonBlankString(payload, "password"); }
        else if (command.equals("tableStart")) { rejectUnknown(payload, Map.of("tableId", true, "intendedPlayerNames", true), command); number(payload, "tableId"); playerNames(list(payload.get("intendedPlayerNames"), "intendedPlayerNames")); }
        else if (command.equals("tableCreate")) { rejectUnknown(payload, Map.of("settings", true, "password", true), command); object(payload.get("settings"), "settings"); if (payload.containsKey("password")) nonBlankString(payload, "password"); }
        else if (Set.of("tableLeave", "tableUnattend", "tableReattend", "tableTerminate", "tableTerminateVote", "gameInitLoad", "gameStateLoad", "loaded", "gameLoaded", "chatRead", "idleReset").contains(command)) {
            var known = Map.of("tableId", true);
            rejectUnknown(payload, known, command); number(payload, "tableId");
        }
        else if (command.equals("tableRestart")) { rejectUnknown(payload, Map.of("tableId", true, "hidePregame", true), command); number(payload, "tableId"); booleanValue(payload, "hidePregame"); }
        else if (command.equals("tableRestartDecline")) { rejectUnknown(payload, Map.of("tableId", true, "declined", true), command); number(payload, "tableId"); booleanValue(payload, "declined"); }
        else if (command.equals("tableSpectate")) { rejectUnknown(payload, Map.of("tableId", true, "shadowingPlayerIndex", true), command); number(payload, "tableId"); if (payload.containsKey("shadowingPlayerIndex")) seat(payload, "shadowingPlayerIndex"); }
        else if (command.equals("pause")) { rejectUnknown(payload, Map.of("tableId", true, "setting", true), command); number(payload, "tableId"); string(payload, "setting"); }
        else if (command.equals("note")) { rejectUnknown(payload, Map.of("tableId", true, "order", true, "text", true), command); number(payload, "tableId"); cardOrder(payload, "order"); string(payload, "text"); }
        else if (command.equals("playerAction")) { rejectUnknown(payload, Map.of("tableId", true, "action", true), command); number(payload, "tableId"); var action = object(payload.get("action"), "action"); String type = string(action, "type"); if (Set.of("play", "discard").contains(type)) { rejectUnknown(action, Map.of("type", true, "order", true), "action"); cardOrder(action, "order"); } else if (type.equals("clue")) { rejectUnknown(action, Map.of("type", true, "receiverIndex", true, "clue", true), "action"); seat(action, "receiverIndex"); clue(object(action.get("clue"), "clue")); } else throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "unknown player action: " + type); }
        else if (command.equals("tableSetOwner")) { rejectUnknown(payload, Map.of("tableId", true, "userId", true), command); number(payload, "tableId"); positiveNumber(payload, "userId"); }
        else if (command.equals("tableUpdate")) { rejectUnknown(payload, Map.of("tableId", true, "settings", true), command); number(payload, "tableId"); object(payload.get("settings"), "settings"); }
        else if (Set.of("friendAdd", "friendRemove", "linkAdd", "linkRemove", "blockAdd", "blockRemove").contains(command)) { rejectUnknown(payload, Map.of("username", true), command); nonBlankString(payload, "username"); }
        else if (command.equals("chatPm")) { rejectUnknown(payload, Map.of("recipient", true, "text", true), command); nonBlankString(payload, "recipient"); string(payload, "text"); }
        else if (command.equals("chatTyping")) { rejectUnknown(payload, Map.of("tableId", true), command); number(payload, "tableId"); }
        else if (Set.of("lobbyLoad", "sync", "linkList").contains(command)) { rejectUnknown(payload, Map.of(), command); }
        else if (command.equals("userInactive")) { rejectUnknown(payload, Map.of("inactive", true), command); booleanValue(payload, "inactive"); }
        else if (command.equals("settings")) { rejectUnknown(payload, Map.of("settings", true, "cleared", true), command); object(payload.get("settings"), "settings"); if (payload.containsKey("cleared")) list(payload.get("cleared"), "cleared"); }
        else if (command.equals("chatPmHistory")) { rejectUnknown(payload, Map.of("recipient", true, "limit", true), command); if (payload.containsKey("recipient")) string(payload, "recipient"); long limit = number(payload, "limit"); if (limit < 1 || limit > 100) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "invalid chat history limit"); }
        else if (command.equals("tableSuggest")) { rejectUnknown(payload, Map.of("tableId", true, "turn", true), command); number(payload, "tableId"); long turn = number(payload, "turn"); if (turn > 512) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "turn is out of range"); }
        else if (command.equals("seedGames")) { rejectUnknown(payload, Map.of("seed", true, "offset", true, "limit", true), command); string(payload, "seed"); long offset = number(payload, "offset"); long limit = number(payload, "limit"); if (limit < 1 || limit > 100 || offset > Integer.MAX_VALUE) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "invalid page"); }
        else if (command.equals("gameHistory")) { rejectUnknown(payload, Map.of("offset", true, "limit", true), command); page(payload, command); }
        else if (command.equals("tagSearch")) { rejectUnknown(payload, Map.of("tag", true), command); string(payload, "tag"); }
        else if (Set.of("tagAdd", "tagRemove").contains(command)) { rejectUnknown(payload, Map.of("tableId", true, "tag", true), command); number(payload, "tableId"); string(payload, "tag"); }
        else if (command.equals("tagRemoveAll")) { rejectUnknown(payload, Map.of("tableId", true), command); number(payload, "tableId"); }
        else if (command.equals("replayCreate")) { rejectUnknown(payload, Map.of("source", true, "visibility", true, "shadowingPlayerIndex", true), command); validateReplaySource(object(payload.get("source"), "source")); validateReplayVisibility(object(payload.get("visibility"), "visibility")); if (payload.containsKey("shadowingPlayerIndex")) seat(payload, "shadowingPlayerIndex"); }
        else if (command.equals("replayAction")) { rejectUnknown(payload, Map.of("tableId", true, "action", true), command); number(payload, "tableId"); validateReplayAction(object(payload.get("action"), "action")); }
        else throw new ProtocolDecodeException(ProtocolDecodeException.Code.UNKNOWN_COMMAND, "unknown command: " + command);
        return new CommandEnvelope(command, payload);
    }

    public static User decodeUser(String json) throws ProtocolDecodeException {
        var object = object(Json.parse(json), "user");
        var known = Map.of("userId", true, "username", true);
        var unknown = unknown(object, known);
        return new User(number(object, "userId"), string(object, "username"), unknown);
    }

    public static Session decodeSession(String json) throws ProtocolDecodeException {
        var object = object(Json.parse(json), "session");
        rejectUnknown(object, Map.of("token", true, "user", true), "session");
        return new Session(string(object, "token"), decodeUser(Json.write(object.get("user"))));
    }

    public static Table decodeTable(String json) throws ProtocolDecodeException {
        var object = object(Json.parse(json), "table");
        var known = Map.of("tableId", true, "name", true, "players", true, "settings", true, "playerNames", true, "owner", true, "phase", true, "spectators", true);
        try {
            var playerValues = object.containsKey("players") ? list(object.get("players"), "players") : list(object.get("playerNames"), "playerNames");
            var players = playerValues.stream().filter(v -> v instanceof Map<?, ?>).map(v -> {
                try { return decodeUser(Json.write(v)); } catch (ProtocolDecodeException e) { throw new Wrapped(e); }
            }).toList();
            var name = object.containsKey("name") ? string(object, "name") : string(object(object.get("settings"), "settings"), "name");
            var phase = object.containsKey("phase") ? string(object, "phase") : "unknown";
            return new Table(new com.pimak.hanab.client.model.Ids.TableId(number(object, "tableId")), phase, ownerName(object.get("owner")), new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(object));
        } catch (Wrapped e) { throw e.cause; }
    }

    private static String ownerName(Object value) {
        if (value instanceof Map<?, ?> map && map.get("username") instanceof String name) return name;
        return value instanceof String name ? name : null;
    }

    public static GameState decodeGameState(String json) throws ProtocolDecodeException {
        var object = object(Json.parse(json), "gameState");
        var events = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (var value : list(object.get("events"), "events")) {
            var event = mapper.valueToTree(value);
            try { com.pimak.hanab.client.game.GameEvent.parse(event); }
            catch (IllegalArgumentException e) { throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "invalid game event", e); }
            events.add(event);
        }
        return new GameState(new com.pimak.hanab.client.model.Ids.TableId(number(object, "tableId")), events, mapper.valueToTree(object));
    }

    public static Replay decodeReplay(String json) throws ProtocolDecodeException {
        var document = decodeReplayDocument(json);
        var unknown = new LinkedHashMap<String, Object>(document.unknownFields());
        if (!document.deck().isEmpty()) unknown.put("deck", document.deck());
        if (!document.options().isEmpty()) unknown.put("options", document.options());
        if (!document.notes().isEmpty()) unknown.put("notes", document.notes());
        if (!document.characters().isEmpty()) unknown.put("characters", document.characters());
        return new Replay(document.seed(), document.playerNames(), document.actions(), unknown);
    }

    public static ReplayDocument decodeReplayDocument(String json) throws ProtocolDecodeException {
        var object = object(Json.parse(json), "replay");
        var known = Map.of("seed", true, "playerNames", true, "players", true, "deck", true, "actions", true, "options", true, "notes", true, "characters", true);
        var seed = object.containsKey("seed") ? java.util.Optional.of(string(object, "seed")) : java.util.Optional.<String>empty();
        var names = new java.util.ArrayList<String>();
        Object playerValues = object.containsKey("playerNames") ? object.get("playerNames") : object.get("players");
        if (playerValues != null) for (var value : list(playerValues, "playerNames")) names.add(value instanceof Map<?, ?> map ? asString(map.get("username"), "playerNames.username") : asString(value, "playerNames"));
        var deck = objectMaps(object.get("deck"), "deck");
        var actions = objectMaps(object.get("actions"), "actions");
        if (actions.size() > 512) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "replay contains too many actions");
        for (var action : actions) { String type=asString(action.get("type"), "action.type"); if (type.equals("play") || type.equals("discard")) { rejectUnknown(action, Map.of("type", true, "order", true), "replay action"); cardOrder(action, "order"); } else if (type.equals("clue")) validateReplayClue(action); else if (type.equals("gameOver")) validateReplayGameOver(action); else throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, "unknown replay action: " + type); }
        var options = object.containsKey("options") ? object(object.get("options"), "options") : Map.<String,Object>of();
        var notes = new java.util.ArrayList<List<String>>();
        if (object.containsKey("notes")) for (var player : list(object.get("notes"), "notes")) { var values=new java.util.ArrayList<String>(); for (var note : list(player, "notes.player")) values.add(asString(note, "notes.value")); notes.add(values); }
        var characters = objectMaps(object.get("characters"), "characters");
        return new ReplayDocument(seed, names, deck, actions, options, notes, characters, unknown(object, known));
    }

    @SuppressWarnings("unchecked") private static List<Map<String,Object>> objectMaps(Object value, String where) throws ProtocolDecodeException {
        if (value == null) return List.of();
        var result = new java.util.ArrayList<Map<String,Object>>();
        for (var item : list(value, where)) result.add(object(item, where));
        return result;
    }

    private static Map<String,Object> object(Object value, String where) throws ProtocolDecodeException {
        if (!(value instanceof Map<?,?> map)) throw new ProtocolDecodeException(ProtocolDecodeException.Code.WRONG_SHAPE, where + " must be an object");
        var result = new LinkedHashMap<String,Object>();
        for (var e : map.entrySet()) if (e.getKey() instanceof String key) result.put(key, e.getValue());
        return result;
    }
    private static List<Object> list(Object value, String where) throws ProtocolDecodeException {
        if (!(value instanceof List<?> list)) throw new ProtocolDecodeException(ProtocolDecodeException.Code.WRONG_SHAPE, where + " must be an array");
        return (List<Object>) list;
    }
    private static void playerNames(List<Object> values) throws ProtocolDecodeException { if(values.isEmpty() || values.size()>6) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE,"intendedPlayerNames must contain 1..6 names"); for(Object value:values) if(!(value instanceof String name) || name.isBlank()) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE,"intendedPlayerNames must contain non-blank names"); }
    private static void rejectUnknown(Map<String,Object> object, Map<String,Boolean> known, String where) throws ProtocolDecodeException {
        if (!unknown(object, known).isEmpty()) throw new ProtocolDecodeException(ProtocolDecodeException.Code.UNKNOWN_FIELD, "unknown field in " + where);
    }
    private static Map<String,Object> unknown(Map<String,Object> object, Map<String,Boolean> known) {
        var result = new LinkedHashMap<String,Object>(); object.forEach((k,v) -> { if (!known.containsKey(k)) result.put(k, v); }); return java.util.Collections.unmodifiableMap(result);
    }
    private static String string(Map<String,Object> object, String key) throws ProtocolDecodeException { return asString(object.get(key), key); }
    private static String nonBlankString(Map<String,Object> object, String key) throws ProtocolDecodeException { String value=string(object,key); if(value.isBlank()) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE,key+" must not be blank"); return value; }
    private static String asString(Object value, String key) throws ProtocolDecodeException { if (!(value instanceof String s)) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, key + " must be a string"); return s; }
    private static long number(Map<String,Object> object, String key) throws ProtocolDecodeException { var value = object.get(key); if (!(value instanceof Integer || value instanceof Long)) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, key + " must be a non-negative integer"); long number=((Number)value).longValue(); if(number<0 || number>9_007_199_254_740_991L) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, key + " must be a safe non-negative integer"); return number; }
    private static long numberOrSigned(Map<String,Object> object, String key) throws ProtocolDecodeException { var value = object.get(key); if (!(value instanceof Integer || value instanceof Long)) throw invalid(key + " must be an integer"); return ((Number) value).longValue(); }
    private static ProtocolDecodeException invalid(String message) { return new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, message); }
    private static long positiveNumber(Map<String,Object> object, String key) throws ProtocolDecodeException { long value=number(object,key); if(value<1 || value>Integer.MAX_VALUE) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE,key+" is out of range"); return value; }
    private static void seat(Map<String,Object> object,String key) throws ProtocolDecodeException { long value=number(object,key); if(value>5) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE,key+" is out of range"); }
    private static void cardOrder(Map<String,Object> object,String key) throws ProtocolDecodeException { long value=number(object,key); if(value>65) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE,key+" is out of range"); }
    private static void validateReplayAction(Map<String,Object> action) throws ProtocolDecodeException {
        String type = string(action, "type");
        switch (type) {
            case "turn" -> { rejectUnknown(action, Map.of("type", true, "turn", true), "replay action"); long turn = number(action, "turn"); if (turn > 512) throw invalid("turn is out of range"); }
            case "sound" -> { rejectUnknown(action, Map.of("type", true, "sound", true), "replay action"); if (!Set.of("buzz", "clap", "holy", "nooo").contains(string(action, "sound"))) throw invalid("unknown replay sound"); }
            case "efficiency" -> { rejectUnknown(action, Map.of("type", true, "modifier", true), "replay action"); long modifier = numberOrSigned(action, "modifier"); if (modifier > 60 || modifier < -60) throw invalid("efficiency modifier is out of range"); }
            case "arrow" -> { rejectUnknown(action, Map.of("type", true, "target", true), "replay action"); if (action.containsKey("target")) validateArrowTarget(object(action.get("target"), "target")); }
            case "hypothetical" -> { rejectUnknown(action, Map.of("type", true, "control", true), "replay action"); validateHypothetical(object(action.get("control"), "control")); }
            default -> throw invalid("unknown replay action: " + type);
        }
    }
    private static void validateReplaySource(Map<String,Object> source) throws ProtocolDecodeException {
        String type = string(source, "type");
        if (type.equals("saved")) { rejectUnknown(source, Map.of("type", true, "gameId", true), "replay source"); positiveNumber(source, "gameId"); }
        else if (type.equals("imported")) { rejectUnknown(source, Map.of("type", true, "game", true), "replay source"); object(source.get("game"), "game"); }
        else throw invalid("unknown replay source: " + type);
    }
    private static void validateReplayClue(Map<String,Object> action) throws ProtocolDecodeException {
        rejectUnknown(action, Map.of("type", true, "target", true, "receiverIndex", true, "clue", true), "replay clue");
        String target = action.containsKey("target") ? "target" : "receiverIndex";
        seat(action, target);
        var clue = object(action.get("clue"), "action.clue");
        String type = string(clue, "type");
        if (type.equals("color")) { rejectUnknown(clue, Map.of("type", true, "color", true), "replay clue"); long value = number(clue, "color"); if (value > 5) throw invalid("replay clue color is out of range"); }
        else if (type.equals("rank")) { rejectUnknown(clue, Map.of("type", true, "rank", true), "replay clue"); long value = number(clue, "rank"); if (value < 1 || value > 5) throw invalid("replay clue rank is out of range"); }
        else throw invalid("unknown replay clue type: " + type);
    }
    private static void validateReplayGameOver(Map<String,Object> action) throws ProtocolDecodeException {
        rejectUnknown(action, Map.of("type", true, "endCondition", true, "playerIndex", true, "votes", true), "replay game over");
        String condition = string(action, "endCondition");
        if (!Set.of("normal", "timeout", "terminatedByPlayer", "terminatedByVote").contains(condition)) throw invalid("unknown game end condition");
        if (action.containsKey("playerIndex")) seat(action, "playerIndex");
        if (action.containsKey("votes")) {
            var votes = list(action.get("votes"), "replay votes");
            for (var vote : votes) if (!(vote instanceof Number number) || number.longValue() < 0 || number.longValue() > 5) throw invalid("vote is out of range");
        }
    }
    private static void validateReplayVisibility(Map<String,Object> visibility) throws ProtocolDecodeException {
        String type = string(visibility, "type");
        if (type.equals("solo")) rejectUnknown(visibility, Map.of("type", true), "replay visibility");
        else if (type.equals("shared")) { rejectUnknown(visibility, Map.of("type", true, "joinExisting", true, "spoilerFree", true), "replay visibility"); booleanValue(visibility, "joinExisting"); booleanValue(visibility, "spoilerFree"); }
        else throw invalid("unknown replay visibility: " + type);
    }
    private static void clue(Map<String,Object> object) throws ProtocolDecodeException { String type=string(object,"type"); if(type.equals("color")){rejectUnknown(object,Map.of("type",true,"color",true),"clue"); long value=number(object,"color"); if(value>5) throw invalid("color is out of range");} else if(type.equals("rank")){rejectUnknown(object,Map.of("type",true,"rank",true),"clue"); long value=number(object,"rank"); if(value<1 || value>5) throw invalid("rank is out of range");} else throw invalid("unknown clue type: "+type); }
    private static void validateArrowTarget(Map<String,Object> target) throws ProtocolDecodeException {
        String type = string(target, "type");
        if (type.equals("card")) { rejectUnknown(target, Map.of("type", true, "order", true), "arrow target"); cardOrder(target, "order"); }
        else if (type.equals("board")) { rejectUnknown(target, Map.of("type", true, "element", true), "arrow target"); String element = string(target, "element"); if (!Set.of("deck", "turn", "score", "maxScore", "clues", "strike1", "strike2", "strike3", "pace", "efficiency", "minEfficiency").contains(element)) throw invalid("unknown board arrow element"); }
        else throw invalid("unknown arrow target: " + type);
    }
    private static void validateHypothetical(Map<String,Object> control) throws ProtocolDecodeException {
        String type = string(control, "type");
        switch (type) {
            case "start", "end", "back" -> rejectUnknown(control, Map.of("type", true), "hypothetical control");
            case "showDrawnCards" -> { rejectUnknown(control, Map.of("type", true, "show", true), "hypothetical control"); booleanValue(control, "show"); }
            case "event" -> { rejectUnknown(control, Map.of("type", true, "event", true), "hypothetical control"); Map<String,Object> event = object(control.get("event"), "hypothetical event"); String eventType = string(event, "type"); if (!Set.of("draw", "play", "discard", "misplay", "clue", "turn", "morph", "unmorph").contains(eventType)) throw invalid("unknown hypothetical event: " + eventType); }
            default -> throw invalid("unknown hypothetical control: " + type);
        }
    }
    private static void page(Map<String,Object> object,String command) throws ProtocolDecodeException { long offset=number(object,"offset"), limit=number(object,"limit"); if(offset<0 || offset>9_007_199_254_740_991L || limit<1 || limit>100) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE,"invalid page in "+command); }
    private static boolean booleanValue(Map<String,Object> object, String key) throws ProtocolDecodeException { var value = object.get(key); if (!(value instanceof Boolean)) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, key + " must be a boolean"); return (Boolean) value; }
    private static int asInt(Object value, String key) throws ProtocolDecodeException { if (!(value instanceof Number n)) throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_VALUE, key + " must be a number"); return n.intValue(); }
    private static final class Wrapped extends RuntimeException { final ProtocolDecodeException cause; Wrapped(ProtocolDecodeException cause) { this.cause = cause; } }
}
