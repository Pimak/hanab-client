package com.pimak.hanab.client.social;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** A server chat entry, retaining its discriminated kind and original payload. */
public record ChatMessage(String text, Instant sentAt, OptionalLongId chatId, OptionalLongId tableId,
                          String kind, Optional<String> author, Optional<String> recipient, JsonNode raw) {
    public ChatMessage {
        if (text == null || text.isEmpty() || sentAt == null || kind == null || kind.isBlank()) throw new IllegalArgumentException("invalid chat message");
        author = Objects.requireNonNull(author, "author"); recipient = Objects.requireNonNull(recipient, "recipient");
        raw = Objects.requireNonNull(raw, "raw").deepCopy();
    }
    @Override public JsonNode raw() { return raw.deepCopy(); }
    public record OptionalLongId(Optional<Long> value) {
        public OptionalLongId { Objects.requireNonNull(value, "value"); }
        public static OptionalLongId empty() { return new OptionalLongId(Optional.empty()); }
        public static OptionalLongId of(long value) { if (value < 0) throw new IllegalArgumentException("id must be non-negative"); return new OptionalLongId(Optional.of(value)); }
    }
    public static ChatMessage from(JsonNode json) {
        if (json == null || !json.isObject()) throw new IllegalArgumentException("invalid chat message");
        if (!json.has("text") || !json.get("text").isTextual() || json.get("text").textValue().isEmpty()
                || !json.has("sentAt") || !json.get("sentAt").isTextual() || !json.has("kind") || !json.get("kind").isObject()
                || !json.get("kind").path("type").isTextual() || json.get("kind").path("type").textValue().isBlank())
            throw new IllegalArgumentException("invalid chat message shape");
        String timestamp=json.get("sentAt").textValue();
        try {
            var kind=json.get("kind"); String type=kind.get("type").textValue();
            Optional<String> author=Optional.empty(), recipient=Optional.empty();
            if (kind.path("author").isObject()) { var value=kind.path("author"); String authorType=value.path("type").asText(""); if(authorType.equals("user")) author=requiredText(value, "username"); else if(authorType.equals("discord") && value.has("username")) author=Optional.ofNullable(value.path("username").isTextual() ? value.path("username").textValue() : null); }
            if (kind.has("recipient")) { if (!kind.get("recipient").isTextual() || kind.get("recipient").textValue().isBlank()) throw new IllegalArgumentException("invalid chat recipient"); recipient=Optional.of(kind.get("recipient").textValue()); }
            return new ChatMessage(json.get("text").textValue(), Instant.parse(timestamp), id(json,"chatId"), id(json,"tableId"), type, author, recipient, json);
        } catch (RuntimeException error) { throw new IllegalArgumentException("invalid chat message", error); }
    }
    private static Optional<String> requiredText(JsonNode node, String key) { var value=node.get(key); if (value == null || !value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("invalid chat " + key); return Optional.of(value.textValue()); }
    private static OptionalLongId id(JsonNode json,String key) {
        var value=json.get(key);
        if (value == null || value.isNull()) return OptionalLongId.empty();
        if (!value.isIntegralNumber() || !value.canConvertToLong()) throw new IllegalArgumentException("invalid chat " + key);
        long minimum = key.equals("tableId") ? 0 : 1;
        if (value.longValue() < minimum) throw new IllegalArgumentException("invalid chat " + key);
        return OptionalLongId.of(value.longValue());
    }
}
