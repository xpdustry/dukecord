// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeTest {

    /** The id Discord's own documentation uses as a worked example. */
    private static final Snowflake DOCUMENTED = Snowflake.of("175928847299117063");

    /** 2^64 - 1: fits in a snowflake, overflows a signed long. */
    private static final String UNSIGNED_MAX = "18446744073709551615";

    @Test
    void parsesUnsignedDecimalAboveLongMaxValue() {
        final var snowflake = Snowflake.of(UNSIGNED_MAX);
        assertThat(snowflake.value()).isEqualTo(-1L);
        assertThat(snowflake.asString()).isEqualTo(UNSIGNED_MAX);
        assertThat(Snowflake.of("9223372036854775808").value()).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void rejectsNonNumericText() {
        assertThatThrownBy(() -> Snowflake.of("not-an-id")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> Snowflake.of("-1")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void asStringIsTheUnsignedDecimalForm() {
        assertThat(DOCUMENTED.asString()).isEqualTo("175928847299117063");
        assertThat(Snowflake.of(0L).asString()).isEqualTo("0");
        assertThat(Snowflake.of(-1L).asString()).isEqualTo(UNSIGNED_MAX);
    }

    @Test
    void createdAtDecodesTheDiscordEpochTimestamp() {
        assertThat(DOCUMENTED.createdAt()).isEqualTo(Instant.parse("2016-04-30T11:18:25.796Z"));
        assertThat(Snowflake.of(0L).createdAt()).isEqualTo(Snowflake.DISCORD_EPOCH);
        assertThat(Snowflake.DISCORD_EPOCH).isEqualTo(Instant.parse("2015-01-01T00:00:00Z"));
    }

    @Test
    void createdAtIsUnsignedForIdsPastTheSignedRange() {
        // A signed shift would yield a negative offset and an instant before the Discord epoch.
        assertThat(Snowflake.of(UNSIGNED_MAX).createdAt()).isAfter(Snowflake.DISCORD_EPOCH);
    }

    @Test
    void fromInstantRoundTripsThroughCreatedAt() {
        final var instant = Instant.parse("2026-09-15T10:30:00.123Z");
        assertThat(Snowflake.fromInstant(instant).createdAt()).isEqualTo(instant);
        assertThat(Snowflake.fromInstant(Snowflake.DISCORD_EPOCH)).isEqualTo(Snowflake.of(0L));
        assertThat(Snowflake.fromInstant(DOCUMENTED.createdAt()).createdAt()).isEqualTo(DOCUMENTED.createdAt());
    }

    @Test
    void comparesUnsigned() {
        final var small = Snowflake.of(1L);
        final var huge = Snowflake.of(UNSIGNED_MAX);
        assertThat(small).isLessThan(huge);
        assertThat(huge).isGreaterThan(small);
        assertThat(small.compareTo(Snowflake.of(1L))).isZero();

        final var sorted = List.of(huge, DOCUMENTED, small).stream().sorted().toList();
        assertThat(sorted).containsExactly(small, DOCUMENTED, huge);
    }

    @Test
    void isAValueWithStringFormToString() {
        assertThat(Snowflake.of("175928847299117063")).isEqualTo(Snowflake.of(175928847299117063L));
        assertThat(Snowflake.of(1L)).hasSameHashCodeAs(Snowflake.of(1L));
        assertThat(DOCUMENTED).hasToString("175928847299117063");
        assertThat(Snowflake.of(-1L)).hasToString(UNSIGNED_MAX);
    }
}
