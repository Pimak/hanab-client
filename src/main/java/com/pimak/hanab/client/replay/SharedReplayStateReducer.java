package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;

/** Applies replay collaboration actions idempotently, without mutating game state. */
public final class SharedReplayStateReducer {
    public SharedReplayState reduce(SharedReplayState previous, ReplayAction action) {
        if (previous == null || action == null) throw new NullPointerException();
        String key = action.raw().toString();
        if (previous.appliedActions().contains(key)) return previous;
        var seen = new HashSet<>(previous.appliedActions()); seen.add(key);
        int leader = previous.leaderTurn();
        int local = previous.localTurn();
        var status = previous.syncStatus();
        String sound = previous.lastSoundName(); Integer efficiency = previous.efficiencyModifier(); JsonNode hypothetical = previous.hypothetical();
        if (action instanceof ReplayAction.Turn turn) { leader = turn.turn(); local = turn.turn(); status = SharedReplayState.SyncStatus.SYNCED; }
        else if (action instanceof ReplayAction.Sound value) sound = value.soundName();
        else if (action instanceof ReplayAction.Efficiency value) efficiency = value.modifier();
        else if (action instanceof ReplayAction.Hypothetical value) hypothetical = value.control();
        return new SharedReplayState(leader, local, status, action, sound, efficiency, hypothetical, seen);
    }
}
