package com.pimak.hanab.client.catalog;

import java.util.Objects;

/** A typed value associated with a Hanab tag. */
public record Tag<T>(String name, T value) {
    public Tag {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        value = Objects.requireNonNull(value, "value");
    }
}
