package com.pimak.hanab.client.social;

import java.util.List;
import java.util.Objects;

/** A slash command not known by this version of the client. */
public record UnknownChatCommand(String name, List<String> arguments, String rawText) implements ParsedChat {
    public UnknownChatCommand {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        Objects.requireNonNull(rawText, "rawText");
    }
}
