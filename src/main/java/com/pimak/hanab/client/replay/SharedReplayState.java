package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

/** Immutable client-side projection of the collaboration state of a shared replay. */
public record SharedReplayState(int leaderTurn, int localTurn, SyncStatus syncStatus,
                                ReplayAction lastAction, String lastSoundName,
                                Integer efficiencyModifier, JsonNode hypothetical,
                                Set<String> appliedActions) {
    public enum SyncStatus { UNKNOWN, SYNCED, BEHIND }

    public SharedReplayState {
        if (leaderTurn < 0 || leaderTurn > 512 || localTurn < 0 || localTurn > 512) throw new IllegalArgumentException("turn out of range");
        if (syncStatus == null || appliedActions == null) throw new NullPointerException();
        if (efficiencyModifier != null && (efficiencyModifier < -60 || efficiencyModifier > 60)) throw new IllegalArgumentException("efficiency modifier out of range");
        lastSoundName = lastSoundName == null ? null : lastSoundName;
        hypothetical = hypothetical == null ? null : hypothetical.deepCopy();
        appliedActions = Set.copyOf(appliedActions);
    }
    public SharedReplayState() { this(0, 0, SyncStatus.UNKNOWN, null, null, null, null, Set.of()); }
    public static SharedReplayState fromSnapshot(JsonNode snapshot) {
        if (snapshot == null || !snapshot.isObject()) throw new IllegalArgumentException("shared replay snapshot must be an object");
        var turn = snapshot.get("turn"); var efficiency = snapshot.get("efficiencyModifier");
        if (turn == null || !turn.isIntegralNumber() || !turn.canConvertToInt() || turn.intValue() < 0 || turn.intValue() > 512
                || efficiency == null || !efficiency.isIntegralNumber() || !efficiency.canConvertToInt() || efficiency.intValue() < -60 || efficiency.intValue() > 60)
            throw new IllegalArgumentException("invalid shared replay snapshot");
        return new SharedReplayState(turn.intValue(), turn.intValue(), SyncStatus.SYNCED, null, null, efficiency.intValue(), null, Set.of());
    }
    @Override public JsonNode hypothetical() { return hypothetical == null ? null : hypothetical.deepCopy(); }
}
