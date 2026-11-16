package com.pimak.hanab.client.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalGameValidatorTest {
    @Test void validatesOnlySafePresentationBounds() {
        assertTrue(LocalGameValidator.cardOrder(4).valid());
        assertFalse(LocalGameValidator.cardOrder(66).valid());
        assertTrue(LocalGameValidator.playerIndex(1, 2).valid());
        assertFalse(LocalGameValidator.playerIndex(2, 2).valid());
    }
    @Test void validatesCluesAndOptionsWithoutClaimingServerAuthority() {
        assertTrue(LocalGameValidator.clue(new Clue.Color(1), 0, 2).valid());
        assertFalse(LocalGameValidator.clue(null, 0, 2).valid());
        assertFalse(LocalGameValidator.options(null).valid());
    }
}
