package com.pimak.hanab.client.social;

import java.util.List;
import java.util.Objects;

/** A recognized Hanab chat command, with lossless source text. */
public record ParsedChatCommand(ChatCommand command, List<String> arguments, String rawText)
        implements ParsedChat {
    public ParsedChatCommand {
        Objects.requireNonNull(command, "command");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        Objects.requireNonNull(rawText, "rawText");
    }

    public String argumentText() {
        var line = rawText.stripLeading();
        var separator = 1;
        while (separator < line.length() && !Character.isWhitespace(line.charAt(separator))) separator++;
        return line.substring(separator).strip();
    }
}
