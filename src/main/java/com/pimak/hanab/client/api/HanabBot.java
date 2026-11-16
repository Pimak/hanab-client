package com.pimak.hanab.client.api;

import java.net.URI;

/** Bot-specific entry point. It always logs in with {@code client=bot}, which marks the account as a bot. */
public final class HanabBot {
    private HanabBot() { }
    public static HanabClient connect(URI server) { return new HanabClientImpl(ClientConfig.bot(server)); }
    public static LoginOptions loginOptions(boolean createAccount) { return LoginOptions.bot(createAccount); }
}
