package com.pimak.hanab.client.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public record ChatCommand(String text, Long tableId) implements OutboundCommand {
    public ChatCommand {
        if (text == null) throw new NullPointerException("text");
        if (tableId != null && (tableId < 0 || tableId > 9_007_199_254_740_991L)) throw new IllegalArgumentException("tableId must be a safe non-negative integer");
    }
    public ChatCommand(String text) { this(text, null); }
    public String commandName() { return "chat"; }
    public Map<String, Object> payload() {
        var result = new LinkedHashMap<String, Object>();
        result.put("text", text);
        if (tableId != null) result.put("tableId", tableId);
        return Collections.unmodifiableMap(result);
    }
}
