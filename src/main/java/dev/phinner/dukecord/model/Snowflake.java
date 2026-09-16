// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** A Discord ID represented as an unsigned 64-bit integer. */
public record Snowflake(long value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Snowflake of(final String value) {
        return new Snowflake(Long.parseUnsignedLong(value));
    }

    @JsonValue
    public String asString() {
        return Long.toUnsignedString(this.value);
    }

    @Override
    public String toString() {
        return this.asString();
    }
}
