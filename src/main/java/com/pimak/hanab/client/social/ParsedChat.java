package com.pimak.hanab.client.social;

/** Result of parsing one chat line. */
public sealed interface ParsedChat permits ChatText, ParsedChatCommand, UnknownChatCommand {
    String rawText();
}
