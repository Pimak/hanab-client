package com.pimak.hanab.client.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Starts a table only if the caller's intended seated roster is still current. */
public record TableStartCommand(long tableId, List<String> intendedPlayerNames) implements OutboundCommand {
    public TableStartCommand { if (tableId < 0 || tableId > 9_007_199_254_740_991L) throw new IllegalArgumentException("tableId must be a safe non-negative integer"); intendedPlayerNames = List.copyOf(intendedPlayerNames); }
    public String commandName() { return "tableStart"; }
    public Map<String, Object> payload() {
        var result = new LinkedHashMap<String,Object>();
        result.put("tableId", tableId); result.put("intendedPlayerNames", intendedPlayerNames);
        return Collections.unmodifiableMap(result);
    }
}
