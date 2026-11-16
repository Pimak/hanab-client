package com.pimak.hanab.client.game;

/** The server's information perspective for a game state. */
public enum Perspective {
    PLAYER, SPECTATOR, SHADOWING, REPLAY, UNKNOWN;

    public static Perspective from(String value) {
        if (value == null) return UNKNOWN;
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "player", "playing" -> PLAYER;
            case "spectator", "spectating" -> SPECTATOR;
            case "shadowing", "shadow" -> SHADOWING;
            case "replay" -> REPLAY;
            default -> UNKNOWN;
        };
    }
}
