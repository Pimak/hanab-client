package com.pimak.hanab.client.social;

/** The conversation in which a social event occurred. */
public sealed interface SocialContext
        permits LobbyContext, TableContext, PrivateContext, GameContext {
    static LobbyContext lobby() { return new LobbyContext(); }
    static TableContext table(long tableId) { return new TableContext(tableId); }
    static PrivateContext privateConversation(String username) { return new PrivateContext(username); }
    static GameContext game(long tableId, long gameId) { return new GameContext(tableId, gameId); }
}
