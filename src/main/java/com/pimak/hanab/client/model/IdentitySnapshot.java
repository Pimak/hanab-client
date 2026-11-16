package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.Optional;
import java.time.Instant;

/** Typed projection of the current identity endpoint, retaining future fields. */
public record IdentitySnapshot(Optional<Ids.UserId> userId, Ids.Username username, Optional<String> token, Optional<Instant> expiresAt, Optional<String> status,
                               Set<String> friends, Set<String> links, Set<String> blocked,
                               JsonNode raw) {
    public IdentitySnapshot {
        if (userId == null || username == null || token == null || expiresAt == null || status == null || friends == null || links == null || blocked == null || raw == null) throw new NullPointerException();
        status = status.filter(value -> !value.isBlank());
        friends = Set.copyOf(friends); links = Set.copyOf(links); blocked = Set.copyOf(blocked); raw = raw.deepCopy();
    }
    @Override public JsonNode raw() { return raw.deepCopy(); }
    public static IdentitySnapshot from(JsonNode json) {
        if (json == null || !json.isObject()) throw new IllegalArgumentException("identity must be an object");
        var user = json.path("user");
        JsonNode source = user.isObject() ? user : json;
        String name = source.path("username").asText("");
        if (name.isBlank()) throw new IllegalArgumentException("identity is missing username");
        var id = source.path("userId");
        Optional<Ids.UserId> userId = id.isIntegralNumber() && id.canConvertToLong() && id.longValue() >= 1 && id.longValue() <= Integer.MAX_VALUE ? Optional.of(new Ids.UserId(id.longValue())) : Optional.empty();
        var expires = json.path("expiresAt");
        Optional<Instant> expiresAt = expires.isTextual() ? Optional.of(Instant.parse(expires.textValue())) : Optional.empty();
        var token = json.path("token");
        var status = json.path("status");
        return new IdentitySnapshot(userId, new Ids.Username(name), token.isTextual() && !token.textValue().isBlank() ? Optional.of(token.textValue()) : Optional.empty(), expiresAt, status.isTextual() && !status.textValue().isBlank() ? Optional.of(status.textValue()) : Optional.empty(),
                names(json, "friends"), names(json, "links"), names(json, "blocked"), json);
    }
    private static Set<String> names(JsonNode json, String field) {
        JsonNode values = json.get(field); if (values == null || values.isNull()) return Set.of();
        if (!values.isArray()) throw new IllegalArgumentException("identity." + field + " must be an array");
        var result = new java.util.LinkedHashSet<String>(); values.forEach(value -> { if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("identity contains invalid " + field); result.add(value.textValue()); }); return result;
    }
}
