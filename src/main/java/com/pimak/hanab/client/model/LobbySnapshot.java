package com.pimak.hanab.client.model;

import java.util.List;

public record LobbySnapshot(List<Table> tables, List<String> onlineUsers) {
    public LobbySnapshot { tables = List.copyOf(tables == null ? List.of() : tables); onlineUsers = List.copyOf(onlineUsers == null ? List.of() : onlineUsers); }
}
