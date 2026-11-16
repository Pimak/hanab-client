package com.pimak.hanab.client.social;

import java.util.Objects;

/** A line which is ordinary chat rather than a recognized command. */
public record ChatText(String rawText) implements ParsedChat {
    public ChatText { Objects.requireNonNull(rawText, "rawText"); }
    public String text() { return rawText; }
}
