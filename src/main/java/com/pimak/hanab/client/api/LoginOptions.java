package com.pimak.hanab.client.api;

public record LoginOptions(boolean createAccount, boolean darkMode, String clientKind, String version) {
    public static LoginOptions app() { return new LoginOptions(false, false, "app", null); }
    public static LoginOptions bot(boolean createAccount) { return new LoginOptions(createAccount, false, "bot", null); }
}
