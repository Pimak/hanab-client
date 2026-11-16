package com.pimak.hanab.client.social;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/** Parses Hanab slash commands without changing the original chat line. */
public final class ChatCommandParser {
    private ChatCommandParser() { }

    public static ParsedChat parse(String rawText) {
        Objects.requireNonNull(rawText, "rawText");
        var line = rawText.stripLeading();
        if (!line.startsWith("/") || line.length() == 1 || Character.isWhitespace(line.charAt(1))) {
            return new ChatText(rawText);
        }
        var tokens = tokens(line.substring(1));
        if (tokens.isEmpty()) return new ChatText(rawText);
        var command = ChatCommand.from(tokens.get(0));
        var arguments = tokens.subList(1, tokens.size());
        if (command == null) return new UnknownChatCommand(tokens.get(0), arguments, rawText);
        return new ParsedChatCommand(command, arguments, rawText);
    }

    /** Parses one of the relationship commands when it has exactly one target. */
    public static Optional<RelationshipAction> parseRelationshipAction(String rawText) {
        var parsed = parse(rawText);
        if (!(parsed instanceof ParsedChatCommand command) || command.arguments().size() != 1) return Optional.empty();
        return switch (command.command()) {
            case FRIEND -> Optional.of(new FriendAction(command.arguments().get(0), rawText));
            case BLOCK -> Optional.of(new BlockAction(command.arguments().get(0), rawText));
            case LINK -> Optional.of(new LinkAction(command.arguments().get(0), rawText));
            case UNLINK -> Optional.of(new UnlinkAction(command.arguments().get(0), rawText));
            case UNFRIEND -> Optional.of(new UnfriendAction(command.arguments().get(0), rawText));
            case UNBLOCK -> Optional.of(new UnblockAction(command.arguments().get(0), rawText));
            case KICK -> Optional.of(new KickAction(command.arguments().get(0), rawText));
            default -> Optional.empty();
        };
    }

    private static ArrayList<String> tokens(String text) {
        var result = new ArrayList<String>();
        var token = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        for (char c : text.toCharArray()) {
            if (escaped) { token.append(c); escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (quote != 0) {
                if (c == quote) quote = 0; else token.append(c);
            } else if (c == '\'' || c == '"') quote = c;
            else if (Character.isWhitespace(c)) { if (!token.isEmpty()) { result.add(token.toString()); token.setLength(0); } }
            else token.append(c);
        }
        if (escaped) token.append('\\');
        if (!token.isEmpty()) result.add(token.toString());
        return result;
    }
}
