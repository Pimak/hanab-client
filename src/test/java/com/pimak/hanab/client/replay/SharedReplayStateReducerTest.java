package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedReplayStateReducerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Test void appliesActionsAndIgnoresDuplicateWireActions() throws Exception {
        var reducer = new SharedReplayStateReducer();
        var state = new SharedReplayState();
        var turn = ReplayAction.from(JSON.readTree("{\"type\":\"turn\",\"turn\":7}"));
        state = reducer.reduce(state, turn);
        state = reducer.reduce(state, turn);
        assertEquals(7, state.leaderTurn());
        assertEquals(7, state.localTurn());
        assertEquals(1, state.appliedActions().size());
        assertEquals(SharedReplayState.SyncStatus.SYNCED, state.syncStatus());
    }

    @Test void preservesUnknownPayloadFieldsByKeepingTheRawAction() throws Exception {
        var action = ReplayAction.from(JSON.readTree("{\"type\":\"sound\",\"sound\":\"buzz\"}"));
        var state = new SharedReplayStateReducer().reduce(new SharedReplayState(), action);
        assertEquals("buzz", state.lastSoundName());
        assertEquals("sound", state.lastAction().type());
    }

    @Test void acceptsTheServerSnapshotAtGameEnd() throws Exception {
        var state = SharedReplayState.fromSnapshot(JSON.readTree("{\"owner\":{\"userId\":1,\"username\":\"A\"},\"turn\":12,\"efficiencyModifier\":2,\"spoilerFree\":false,\"restartDeclined\":[],\"canPlayAgain\":true,\"replayStartedAt\":\"2026-01-01T00:00:00.000Z\"}"));
        assertEquals(12, state.leaderTurn());
        assertEquals(2, state.efficiencyModifier());
    }
}
