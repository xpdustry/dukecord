// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Snowflake;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class MessageQueryTest {

    private static final Snowflake CURSOR = Snowflake.of("175928847299117063");

    @Test
    void latestCarriesNoCursor() {
        final var query = MessageQuery.latest(50);
        assertThat(query.before()).isEmpty();
        assertThat(query.after()).isEmpty();
        assertThat(query.around()).isEmpty();
        assertThat(query.limit()).isEqualTo(50);
        assertThat(query.toQueryParams()).containsExactly(entry("limit", "50"));
    }

    @Test
    void beforeAfterAndAroundEachSetTheirOwnCursor() {
        assertThat(MessageQuery.before(CURSOR, 10).toQueryParams())
                .containsExactly(entry("before", "175928847299117063"), entry("limit", "10"));
        assertThat(MessageQuery.after(CURSOR, 10).toQueryParams())
                .containsExactly(entry("after", "175928847299117063"), entry("limit", "10"));
        assertThat(MessageQuery.around(CURSOR, 10).toQueryParams())
                .containsExactly(entry("around", "175928847299117063"), entry("limit", "10"));
    }

    @Test
    void everyCursorIsRenderedWhenAllThreeAreSet() {
        final var query = new MessageQuery(Optional.of(CURSOR), Optional.of(CURSOR), Optional.of(CURSOR), 1);
        assertThat(query.toQueryParams()).containsOnlyKeys("before", "after", "around", "limit");
    }

    @Test
    void limitMustBeBetweenOneAndAHundred() {
        assertThat(MessageQuery.MAX_LIMIT).isEqualTo(100);
        assertThat(MessageQuery.latest(1).limit()).isEqualTo(1);
        assertThat(MessageQuery.latest(MessageQuery.MAX_LIMIT).limit()).isEqualTo(100);

        assertThatThrownBy(() -> MessageQuery.latest(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 100");
        assertThatThrownBy(() -> MessageQuery.latest(101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MessageQuery.before(CURSOR, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isAValue() {
        assertThat(MessageQuery.latest(10)).isEqualTo(MessageQuery.latest(10));
        assertThat(MessageQuery.latest(10)).hasSameHashCodeAs(MessageQuery.latest(10));
        assertThat(MessageQuery.latest(10)).isNotEqualTo(MessageQuery.latest(11));
    }
}
