package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import com.pimak.hanab.client.model.Ids;

/**
 * Explicit compatibility seam for integrations that still need protocol JSON.
 *
 * <p>Application code should depend on {@link HanabClient}; this interface is
 * intentionally separate so a protocol adapter can opt into the wire format
 * without making JSON part of the domain-facing client contract.</p>
 */
public interface RawProtocolClient {
    CompletionStage<Void> command(String command, JsonNode payload);
    CompletionStage<ServerEvent> sendAndWait(String command, JsonNode payload,
                                              Predicate<ServerEvent> completion, Duration timeout);
    /** Returns the underlying cancellable future; cancellation also releases the serialized wait slot. */
    default CompletableFuture<ServerEvent> sendAndWaitCancellable(String command, JsonNode payload,
                                                                    Predicate<ServerEvent> completion, Duration timeout) {
        return sendAndWait(command, payload, completion, timeout).toCompletableFuture();
    }
    CompletionStage<Void> createTable(JsonNode settings, String password);
    CompletionStage<Void> clue(Ids.TableId tableId, int receiverIndex, JsonNode clue);
    CompletionStage<Void> createReplay(JsonNode source, JsonNode visibility);
    CompletionStage<Void> replayAction(Ids.TableId tableId, JsonNode action);
    CompletionStage<Void> updateTable(Ids.TableId tableId, JsonNode settings);
    CompletionStage<Void> updateSettings(JsonNode settings, List<String> cleared);
}
