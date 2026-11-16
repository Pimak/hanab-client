package com.pimak.hanab.client.model;

import java.util.Map;
import java.util.Objects;

/** A user as identified by the server. */
public record User(long userId, String username, Map<String, Object> unknownFields) {
    public User {
        if (userId < 0) throw new IllegalArgumentException("userId must be non-negative");
        username = Objects.requireNonNull(username, "username");
        if (username.isBlank()) throw new IllegalArgumentException("username must not be blank");
        unknownFields = ModelSupport.immutableMap(unknownFields);
    }

    public User(long userId, String username) { this(userId, username, Map.of()); }
}
