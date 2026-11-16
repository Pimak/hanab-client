package com.pimak.hanab.client.game;

/** The public identity of a card. A server may omit this value for hidden cards. */
public record CardIdentity(int suitIndex, int rank) { }
