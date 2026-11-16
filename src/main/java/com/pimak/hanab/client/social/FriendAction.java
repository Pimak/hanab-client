package com.pimak.hanab.client.social;

import java.util.Objects;

public record FriendAction(String username, String rawText) implements RelationshipAction {
    public FriendAction {
        requireUsername(username);
        Objects.requireNonNull(rawText, "rawText");
    }
    public FriendAction(String username) { this(username, "/friend " + username); }
    private static void requireUsername(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("username"); }
}
