package com.pimak.hanab.client.api;

public record LoginOptions(boolean createAccount) {
    public static LoginOptions bot(boolean createAccount) { return new LoginOptions(createAccount); }
}
