package com.pimak.hanab.client.api;

import com.pimak.hanab.client.protocol.WireMessage;
import com.pimak.hanab.client.game.GameEvent;
import com.pimak.hanab.client.catalog.GamePage;
import com.pimak.hanab.client.model.UserPresence;
import com.pimak.hanab.client.model.LobbyStatus;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.OptionalInt;
import com.pimak.hanab.client.social.ChatMessage;
import com.pimak.hanab.client.social.TypingIndicator;

public record ServerEvent(Instant receivedAt, WireMessage message) {
    public ServerEvent { if (receivedAt == null || message == null) throw new NullPointerException(); }
    public Optional<TableStarted> tableStarted() {
        if (!message.command().equals("tableStarted")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "stage"), "tableStarted");
        var stage = requiredText(message.payload(), "stage");
        var value = switch (stage) {
            case "justStarted" -> TableStarted.Stage.JUST_STARTED;
            case "playing" -> TableStarted.Stage.PLAYING;
            case "replay" -> TableStarted.Stage.REPLAY;
            default -> throw new IllegalArgumentException("unknown table stage: " + stage);
        };
        return Optional.of(new TableStarted(tableId(message.payload()), value));
    }
    public Optional<TableOwnerChanged> tableOwnerChanged() {
        if (!message.command().equals("tableOwner")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "owner"), "tableOwner");
        var owner = message.payload().get("owner");
        if (owner == null || !owner.isObject()) throw new IllegalArgumentException("tableOwner is missing owner");
        try { return Optional.of(new TableOwnerChanged(tableId(message.payload()), com.pimak.hanab.client.protocol.ProtocolPayloadCodec.decodeUser(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(owner)))); }
        catch (Exception error) { throw new IllegalArgumentException("invalid table owner", error); }
    }
    public Optional<SpectatorsUpdated> spectatorsUpdated() {
        if (!message.command().equals("spectators")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "spectators"), "spectators");
        var values = message.payload().get("spectators");
        if (values == null || !values.isArray()) throw new IllegalArgumentException("spectators event is missing spectators");
        var result = new java.util.ArrayList<com.pimak.hanab.client.game.Spectator>();
        values.forEach(value -> result.add(com.pimak.hanab.client.game.Spectator.from(value)));
        return Optional.of(new SpectatorsUpdated(tableId(message.payload()), result));
    }
    public Optional<GamePause> pauseState() {
        if (!message.command().equals("pause")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "pausingPlayerIndex", "queued"), "pause");
        var pause = message.payload();
        java.util.OptionalInt player = pause.has("pausingPlayerIndex") && !pause.get("pausingPlayerIndex").isNull()
                ? java.util.OptionalInt.of(requiredInt(pause, "pausingPlayerIndex", 0, 5)) : java.util.OptionalInt.empty();
        var queued = pause.get("queued");
        if (queued == null || !queued.isBoolean()) throw new IllegalArgumentException("pause event is missing queued");
        return Optional.of(new GamePause(tableId(pause), player, queued.booleanValue()));
    }
    public Optional<GameVotes> votes() {
        if (!message.command().equals("votes")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "voted", "numVotesNeeded"), "votes");
        var voted = message.payload().get("voted");
        if (voted == null || !voted.isBoolean()) throw new IllegalArgumentException("votes event is missing voted");
        return Optional.of(new GameVotes(tableId(message.payload()), voted.booleanValue(), requiredInt(message.payload(), "numVotesNeeded", 0, 6)));
    }
    public Optional<GameEvent> gameEvent() {
        if (!message.command().equals("gameEvent")) return Optional.empty();
        var event = message.payload().get("event");
        return event == null || !event.isObject() ? Optional.empty() : Optional.of(GameEvent.parse(event));
    }
    public Optional<GameEventMessage> gameEventMessage() {
        if (!message.command().equals("gameEvent")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "event"), "gameEvent");
        var tableId = message.payload().get("tableId");
        var event = message.payload().get("event");
        if (tableId == null || !tableId.isIntegralNumber() || !tableId.canConvertToLong() || tableId.longValue() < 0 || tableId.longValue() > 9_007_199_254_740_991L || event == null || !event.isObject())
            throw new IllegalArgumentException("gameEvent envelope is invalid");
        return Optional.of(new GameEventMessage(new com.pimak.hanab.client.model.Ids.TableId(tableId.longValue()), GameEvent.parse(event)));
    }
    public Optional<ReplayActionMessage> replayAction() {
        if (!message.command().equals("replayAction")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "action"), "replayAction");
        var tableId = message.payload().get("tableId");
        var action = message.payload().get("action");
        if (tableId == null || !tableId.isIntegralNumber() || !tableId.canConvertToLong() || tableId.longValue() < 0 || tableId.longValue() > 9_007_199_254_740_991L || action == null || !action.isObject())
            throw new IllegalArgumentException("replayAction envelope is invalid");
        return Optional.of(new ReplayActionMessage(new com.pimak.hanab.client.model.Ids.TableId(tableId.longValue()), com.pimak.hanab.client.replay.ReplayAction.from(action)));
    }
    public Optional<TurnSuggestion> turnSuggestion() {
        if (!message.command().equals("turnSuggestion")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "username", "turn"), "turnSuggestion");
        return Optional.of(new TurnSuggestion(tableId(message.payload()), requiredText(message.payload(), "username"), requiredInt(message.payload(), "turn", 0, 512)));
    }
    public Optional<RestartDeclined> restartDeclined() {
        if (!message.command().equals("restartDeclined")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("tableId", "restartDeclined"), "restartDeclined");
        var values = message.payload().path("restartDeclined");
        if (!values.isArray()) throw new IllegalArgumentException("restartDeclined event is missing restartDeclined");
        var names = new java.util.ArrayList<String>(); values.forEach(value -> { if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("invalid declined username"); names.add(value.textValue()); });
        return Optional.of(new RestartDeclined(tableId(message.payload()), names));
    }
    public Optional<GameFinished> gameFinished() {
        if (!message.command().equals("gameFinished")) return Optional.empty();
        var payload = message.payload();
        rejectUnknown(payload, java.util.Set.of("tableId", "gameId", "sharedReplay"), "gameFinished");
        var id = payload.get("gameId");
        if (id != null && id.isNull()) throw new IllegalArgumentException("gameFinished.gameId must be absent, not null");
        Optional<com.pimak.hanab.client.model.Ids.GameId> gameId = id == null ? Optional.empty() : Optional.of(new com.pimak.hanab.client.model.Ids.GameId(requiredInt(payload, "gameId", 1, Integer.MAX_VALUE)));
        return Optional.of(new GameFinished(tableId(payload), gameId, payload.path("sharedReplay")));
    }
    private static com.pimak.hanab.client.model.Ids.TableId tableId(com.fasterxml.jackson.databind.JsonNode payload) { var value=payload.get("tableId"); if(value==null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue()<0 || value.longValue()>9_007_199_254_740_991L) throw new IllegalArgumentException("event is missing tableId"); return new com.pimak.hanab.client.model.Ids.TableId(value.longValue()); }
    private static int requiredInt(com.fasterxml.jackson.databind.JsonNode payload, String name, int min, int max) { var value=payload.get(name); if(value==null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue()<min || value.intValue()>max) throw new IllegalArgumentException("invalid "+name); return value.intValue(); }
    private static String requiredText(com.fasterxml.jackson.databind.JsonNode payload, String name) { var value=payload.get(name); if(value==null || !value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("invalid "+name); return value.textValue(); }
    public Optional<GamePage> gamePage() {
        if (!message.command().equals("gameHistory")) return Optional.empty();
        return Optional.of(GamePage.from(message.payload()));
    }
    public Optional<SeedGamesPage> seedGames() {
        if (!message.command().equals("seedGames")) return Optional.empty();
        var seed = message.payload().path("seed");
        if (!seed.isTextual()) throw new IllegalArgumentException("seedGames event is missing seed");
        return Optional.of(new SeedGamesPage(seed.textValue(), GamePage.from(message.payload())));
    }
    public Optional<LobbySound> lobbySound() {
        if (!message.command().equals("lobbySound")) return Optional.empty();
        var sound = message.payload().path("sound");
        if (!sound.isTextual()) throw new IllegalArgumentException("lobbySound event is missing sound");
        return Optional.of(switch (sound.textValue()) {
            case "shutdown" -> LobbySound.SHUTDOWN;
            case "someoneJoined" -> LobbySound.SOMEONE_JOINED;
            default -> throw new IllegalArgumentException("unknown lobby sound: " + sound.textValue());
        });
    }
    /** Returns the wire sound name, including values unknown to this client version. */
    public Optional<String> lobbySoundName() {
        if (!message.command().equals("lobbySound")) return Optional.empty();
        var sound = message.payload().path("sound");
        if (!sound.isTextual() || sound.textValue().isBlank()) throw new IllegalArgumentException("lobbySound event is missing sound");
        return Optional.of(sound.textValue());
    }
    /** Returns a chat sound identifier without imposing an audio implementation. */
    public Optional<String> chatSoundName() {
        if (!message.command().equals("chatSound")) return Optional.empty();
        var sound = message.payload().path("sound");
        if (!sound.isTextual() || sound.textValue().isBlank()) throw new IllegalArgumentException("chatSound event is missing sound");
        return Optional.of(sound.textValue());
    }
    public Optional<List<UserPresence>> userList() {
        if (!message.command().equals("userList")) return Optional.empty();
        var users = message.payload().path("users");
        if (!users.isArray()) throw new IllegalArgumentException("userList event is missing users");
        var result = new java.util.ArrayList<UserPresence>(); users.forEach(user -> result.add(UserPresence.from(user)));
        return Optional.of(List.copyOf(result));
    }
    public Optional<LobbyStatus> lobbyStatus() {
        if (!message.command().equals("lobbyStatus")) return Optional.empty();
        var type = message.payload().path("type").asText("");
        var payload = message.payload();
        return Optional.of(switch (type) {
            case "loading" -> { rejectUnknown(payload, java.util.Set.of("type"), "lobbyStatus"); yield new LobbyStatus.Loading(); }
            case "ready" -> { rejectUnknown(payload, java.util.Set.of("type", "numGames"), "lobbyStatus"); var count = payload.get("numGames"); if (count == null || !count.isIntegralNumber() || count.longValue() < 0 || count.longValue() > 9_007_199_254_740_991L) throw new IllegalArgumentException("lobbyStatus.numGames must be a valid count"); yield new LobbyStatus.Ready(count.longValue()); }
            case "error" -> { rejectUnknown(payload, java.util.Set.of("type", "message", "code", "diagnosticId"), "lobbyStatus"); var value = payload.get("message"); if (value == null || !value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("lobbyStatus.error.message must be non-blank text"); yield new LobbyStatus.Error(value.textValue(), textOrNull(payload, "code"), textOrNull(payload, "diagnosticId")); }
            default -> throw new IllegalArgumentException("unknown lobby status: " + type);
        });
    }
    public Optional<List<String>> friends() { return relationshipList("friends", "friends"); }
    public Optional<List<String>> blocked() { return relationshipList("blocked", "blocked"); }
    private Optional<List<String>> relationshipList(String command, String field) {
        if (!message.command().equals(command)) return Optional.empty();
        var payload = message.payload(); rejectUnknown(payload, java.util.Set.of(field), command); var values = payload.path(field); if (!values.isArray()) throw new IllegalArgumentException(command + " event is missing " + field);
        var result = new java.util.ArrayList<String>(); values.forEach(value -> { if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException(command + " contains an invalid username"); result.add(value.textValue()); });
        return Optional.of(List.copyOf(result));
    }
    private static String textOrNull(com.fasterxml.jackson.databind.JsonNode payload, String field) { var value = payload.get(field); if (value == null) return null; if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException(field + " must be non-blank text"); return value.textValue(); }
    private static void rejectUnknown(com.fasterxml.jackson.databind.JsonNode payload, java.util.Set<String> known, String where) { var names = payload.fieldNames(); while (names.hasNext()) { if (!known.contains(names.next())) throw new IllegalArgumentException("unknown field in " + where); } }
    public OptionalLong userRemoved() {
        if (!message.command().equals("userRemoved")) return OptionalLong.empty();
        var id=message.payload().path("userId"); if (!id.canConvertToLong() || id.longValue()<1) throw new IllegalArgumentException("userRemoved event is missing userId");
        return OptionalLong.of(id.longValue());
    }
    public Optional<ChatMessage> chat() {
        if (!message.command().equals("chat")) return Optional.empty();
        return Optional.of(ChatMessage.from(message.payload()));
    }
    public Optional<ChatDeleted> chatDeleted() {
        if (!message.command().equals("chatDeleted")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("chatId"), "chatDeleted");
        return Optional.of(new ChatDeleted(requiredLong(message.payload(), "chatId", 1)));
    }
    public Optional<TypingIndicator> typingIndicator() {
        if (!message.command().equals("chatTyping")) return Optional.empty();
        rejectUnknown(message.payload(), java.util.Set.of("username", "tableId", "typing"), message.command());
        var typing = message.payload().get("typing");
        if (typing == null || !typing.isBoolean()) throw new IllegalArgumentException("typing event is missing typing");
        return Optional.of(new TypingIndicator(requiredText(message.payload(), "username"), Optional.of(tableId(message.payload())), typing.booleanValue()));
    }
    private static Optional<com.pimak.hanab.client.model.Ids.TableId> optionalTableId(com.fasterxml.jackson.databind.JsonNode payload) {
        var value = payload.get("tableId");
        if (value == null || value.isNull()) return Optional.empty();
        return Optional.of(tableId(payload));
    }
    public Optional<List<ChatMessage>> chatList() {
        if (!message.command().equals("chatList") && !message.command().equals("chatPmHistory")) return Optional.empty();
        if (message.command().equals("chatList")) {
            rejectUnknown(message.payload(), java.util.Set.of("messages", "numUnread"), "chatList");
            requiredLong(message.payload(), "numUnread", 0);
        } else {
            rejectUnknown(message.payload(), java.util.Set.of("messages"), "chatPmHistory");
        }
        var messages=message.payload().get("messages"); if(messages == null || !messages.isArray()) throw new IllegalArgumentException("chat history is missing messages");
        var result=new java.util.ArrayList<ChatMessage>(); messages.forEach(value -> result.add(ChatMessage.from(value))); return Optional.of(List.copyOf(result));
    }
    public OptionalLong chatUnreadCount() {
        if (!message.command().equals("chatList")) return OptionalLong.empty();
        rejectUnknown(message.payload(), java.util.Set.of("messages", "numUnread"), "chatList");
        return OptionalLong.of(requiredLong(message.payload(), "numUnread", 0));
    }
    private static long requiredLong(com.fasterxml.jackson.databind.JsonNode payload, String name, long min) {
        var value = payload.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < min) throw new IllegalArgumentException("invalid " + name);
        return value.longValue();
    }
}
