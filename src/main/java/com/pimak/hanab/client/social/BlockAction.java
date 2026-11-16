package com.pimak.hanab.client.social;

import java.util.Objects;

public record BlockAction(String username, String rawText) implements RelationshipAction {
    public BlockAction {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username");
        Objects.requireNonNull(rawText, "rawText");
    }
    public BlockAction(String username) { this(username, "/block " + username); }
}
