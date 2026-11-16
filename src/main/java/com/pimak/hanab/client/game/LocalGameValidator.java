package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** Small presentation-time validator that deliberately does not replace server authority. */
public final class LocalGameValidator {
    private LocalGameValidator() { }

    public static LocalValidationResult cardOrder(int order) {
        return order >= 0 && order <= 65 ? LocalValidationResult.accepted() : LocalValidationResult.rejected("card order is outside the local protocol range");
    }
    public static LocalValidationResult playerIndex(int index, int playerCount) {
        return playerCount >= 2 && playerCount <= 6 && index >= 0 && index < playerCount
                ? LocalValidationResult.accepted() : LocalValidationResult.rejected("player index is outside the visible player range");
    }
    public static LocalValidationResult clue(Clue clue, int receiverIndex, int playerCount) {
        if (clue == null) return LocalValidationResult.rejected("clue is missing");
        var seat = playerIndex(receiverIndex, playerCount);
        if (!seat.valid()) return seat;
        return clue instanceof Clue.Color || clue instanceof Clue.Rank ? LocalValidationResult.accepted() : LocalValidationResult.rejected("unknown clue type");
    }
    public static LocalValidationResult options(JsonNode options) {
        if (options == null || !options.isObject()) return LocalValidationResult.rejected("options must be an object");
        try { com.pimak.hanab.client.model.GameOptions.fromJson(options); return LocalValidationResult.accepted(); }
        catch (RuntimeException error) { return LocalValidationResult.rejected(error.getMessage() == null ? "invalid game options" : error.getMessage()); }
    }
}
