package com.pimak.hanab.client.api;

/** Notification that a previously delivered chat entry was removed. */
public record ChatDeleted(long chatId) {
    public ChatDeleted { if (chatId < 1) throw new IllegalArgumentException("chat id must be positive"); }
}
