package com.pimak.hanab.client.social;

import com.pimak.hanab.client.model.User;
import java.util.Objects;

/** A user's typing state in a social context. */
public record TypingNotification(SocialContext context, User user, boolean typing) {
    public TypingNotification {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(user, "user");
    }

    public TypingNotification(SocialContext context, User user, TypingState state) {
        this(context, user, Objects.requireNonNull(state, "state") == TypingState.TYPING);
    }

    public TypingState state() { return typing ? TypingState.TYPING : TypingState.STOPPED; }
}
