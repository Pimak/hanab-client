package com.pimak.hanab.client.game;

/** Non-authoritative validation hint; only the server decides whether an action is legal. */
public record LocalValidationResult(boolean valid, String reason) {
    public LocalValidationResult { if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason must not be blank"); }
    public static LocalValidationResult accepted() { return new LocalValidationResult(true, "locally valid"); }
    public static LocalValidationResult rejected(String reason) { return new LocalValidationResult(false, reason); }
}
