package com.pimak.hanab.client.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public record TableJoinCommand(long tableId, String password) implements OutboundCommand {
    public TableJoinCommand {
        if (tableId < 0 || tableId > 9_007_199_254_740_991L) throw new IllegalArgumentException("tableId must be a safe non-negative integer");
    }
    public TableJoinCommand(long tableId) { this(tableId, null); }
    public String commandName() { return "tableJoin"; }
    public Map<String, Object> payload() {
        var result = new LinkedHashMap<String, Object>();
        result.put("tableId", tableId);
        if (password != null) result.put("password", password);
        return Collections.unmodifiableMap(result);
    }
}
