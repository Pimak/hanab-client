package com.pimak.hanab.client.model;

import java.time.Instant;

public record Session(Ids.UserId userId, Ids.Username username, Instant authenticatedAt, String clientKind) {
    public Session { if (userId == null || username == null || authenticatedAt == null) throw new NullPointerException(); }
    public Session(String ignoredToken, User user) { this(new Ids.UserId(user.userId()), new Ids.Username(user.username()), Instant.now(), "app"); }
}
