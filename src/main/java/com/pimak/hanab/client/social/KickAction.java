package com.pimak.hanab.client.social;

import java.util.Objects;

public record KickAction(String username, String rawText) implements RelationshipAction {
    public KickAction {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username");
        Objects.requireNonNull(rawText, "rawText");
    }
    public KickAction(String username) { this(username, "/kick " + username); }
}
