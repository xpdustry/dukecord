// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.time.Instant;

/** A Discord id: a 64-bit unsigned integer carrying its creation timestamp. */
public record Snowflake(long value) implements Comparable<Snowflake> {

    public static final Instant DISCORD_EPOCH = Instant.ofEpochMilli(1_420_070_400_000L);

    public static Snowflake of(final long value) {
        return new Snowflake(value);
    }

    /** Parses the unsigned decimal form Discord uses in JSON. */
    public static Snowflake of(final String value) {
        return new Snowflake(Long.parseUnsignedLong(value));
    }

    /** A synthetic snowflake for the given instant, useful as a pagination cursor. */
    public static Snowflake fromInstant(final Instant instant) {
        return new Snowflake((instant.toEpochMilli() - DISCORD_EPOCH.toEpochMilli()) << 22L);
    }

    public Instant createdAt() {
        return DISCORD_EPOCH.plusMillis(this.value >>> 22L);
    }

    /** The unsigned decimal form Discord expects in paths and JSON. */
    public String asString() {
        return Long.toUnsignedString(this.value);
    }

    @Override
    public int compareTo(final Snowflake other) {
        return Long.compareUnsigned(this.value, other.value);
    }

    @Override
    public String toString() {
        return this.asString();
    }
}
