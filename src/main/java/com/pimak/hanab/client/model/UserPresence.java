package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/** Presence information from the lobby user list. */
public record UserPresence(Ids.UserId userId, Ids.Username username, String activity,
                           boolean hGroupConventions, Integer hGroupLevel, boolean bot, boolean inactive,
                           JsonNode raw) {
    public UserPresence {
        Objects.requireNonNull(userId, "userId"); Objects.requireNonNull(username, "username");
        if (activity == null || activity.isBlank()) throw new IllegalArgumentException("activity must not be blank");
        Objects.requireNonNull(raw, "raw"); raw = raw.deepCopy();
    }
    public static UserPresence from(JsonNode json) {
        if (json == null || !json.isObject()) throw new IllegalArgumentException("invalid user presence");
        var userId=json.get("userId"); var username=json.get("username"); var activity=json.get("activity");
        if (userId == null || !userId.isIntegralNumber() || !userId.canConvertToLong() || username == null || !username.isTextual() || username.textValue().isBlank() || activity == null || !activity.isObject() || !activity.path("type").isTextual() || activity.path("type").textValue().isBlank()) throw new IllegalArgumentException("invalid user presence identity");
        Integer level = null;
        if (json.has("hGroupLevel") && !json.get("hGroupLevel").isNull()) { if (!json.get("hGroupLevel").isIntegralNumber() || !json.get("hGroupLevel").canConvertToInt() || json.get("hGroupLevel").intValue() < 0) throw new IllegalArgumentException("invalid H-Group level"); level = json.get("hGroupLevel").intValue(); }
        return new UserPresence(new Ids.UserId(userId.longValue()), new Ids.Username(username.textValue()), activity.path("type").textValue(), booleanField(json, "hGroupConventions"), level, booleanField(json, "bot"), booleanField(json, "inactive"), json);
    }
    private static boolean booleanField(JsonNode json, String name) { var value=json.get(name); if (value == null || !value.isBoolean()) throw new IllegalArgumentException(name + " must be boolean"); return value.booleanValue(); }
}
