package com.pimak.hanab.client.api;
import com.pimak.hanab.client.model.Ids;
import java.util.List;
public record RestartDeclined(Ids.TableId tableId, List<String> usernames) {
    public RestartDeclined { if (tableId == null || usernames == null || usernames.size() > 6 || usernames.stream().anyMatch(v -> v == null || v.isBlank())) throw new IllegalArgumentException("invalid restart decline"); usernames = List.copyOf(usernames); }
}
