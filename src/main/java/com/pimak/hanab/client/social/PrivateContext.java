package com.pimak.hanab.client.social;

import java.util.Objects;

/** A private conversation with one user. */
public record PrivateContext(String username) implements SocialContext {
    public PrivateContext { if (username == null || username.isBlank()) throw new IllegalArgumentException("username"); }
}
