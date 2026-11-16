package com.pimak.hanab.client.social;

import com.pimak.hanab.client.model.User;
import java.util.Objects;

/** An immutable social message. {@link #rawText()} is never normalized. */
public record SocialMessage(SocialContext context, User sender, String rawText) {
    public SocialMessage {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(rawText, "rawText");
    }

    public String text() { return rawText; }
}
