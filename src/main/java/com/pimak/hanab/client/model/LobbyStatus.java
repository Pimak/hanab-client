package com.pimak.hanab.client.model;

import java.util.Optional;

/** Typed lobby loading status sent by the bot protocol. */
public sealed interface LobbyStatus permits LobbyStatus.Loading, LobbyStatus.Ready, LobbyStatus.Error {
    record Loading() implements LobbyStatus { }
    record Ready(long numGames) implements LobbyStatus {
        public Ready { if (numGames < 0) throw new IllegalArgumentException("numGames must be non-negative"); }
    }
    record Error(String message, String code, String diagnosticId) implements LobbyStatus {
        public Error { if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank"); }
        public Error(String message) { this(message, null, null); }
    }
}
