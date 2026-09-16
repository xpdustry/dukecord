// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A tri-state value mirroring Discord's field conventions: the key may be absent ("unchanged"), present with
 * {@code null} ("cleared"), or present with a value. Named after Discord4J's {@code Possible}, but folds the nullable
 * case in instead of nesting {@code Possible<Optional<T>>}.
 */
public sealed interface Possible<T> {

    record Absent<T>() implements Possible<T> {}

    record Null<T>() implements Possible<T> {}

    record Present<T>(T value) implements Possible<T> {
        public Present {
            Objects.requireNonNull(value, "value");
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Possible<T> absent() {
        return (Possible<T>) Holder.ABSENT;
    }

    @SuppressWarnings("unchecked")
    static <T> Possible<T> ofNull() {
        return (Possible<T>) Holder.NULL;
    }

    static <T> Possible<T> of(final T value) {
        return new Present<>(value);
    }

    static <T> Possible<T> ofOptional(final Optional<T> value) {
        return value.<Possible<T>>map(Possible::of).orElseGet(Possible::absent);
    }

    default boolean isAbsent() {
        return this instanceof Absent;
    }

    default boolean isNull() {
        return this instanceof Null;
    }

    default boolean isPresent() {
        return this instanceof Present;
    }

    /** The value if present; empty for both absent and null. */
    default Optional<T> toOptional() {
        return this instanceof Present<T>(final T value) ? Optional.of(value) : Optional.empty();
    }

    default <R> Possible<R> map(final Function<? super T, ? extends R> mapper) {
        return switch (this) {
            case Absent<T> ignored -> Possible.absent();
            case Null<T> ignored -> Possible.ofNull();
            case Present<T>(final T value) -> Possible.of(mapper.apply(value));
        };
    }

    final class Holder {
        private static final Possible<?> ABSENT = new Absent<>();
        private static final Possible<?> NULL = new Null<>();

        private Holder() {}
    }
}
