// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.Snowflake;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class MemberQueryTest {

    private static final Snowflake CURSOR = Snowflake.of("302096570298990593");

    @Test
    void listCarriesOnlyALimit() {
        final var query = MemberQuery.list(100);
        assertThat(query.search()).isEmpty();
        assertThat(query.after()).isEmpty();
        assertThat(query.isSearch()).isFalse();
        assertThat(query.toQueryParams()).containsExactly(entry("limit", "100"));
    }

    @Test
    void afterPaginatesByUserId() {
        final var query = MemberQuery.after(CURSOR, 50);
        assertThat(query.after()).contains(CURSOR);
        assertThat(query.isSearch()).isFalse();
        assertThat(query.toQueryParams()).containsExactly(entry("after", "302096570298990593"), entry("limit", "50"));
    }

    @Test
    void searchSendsTheQueryParameter() {
        final var query = MemberQuery.search("phin", 10);
        assertThat(query.search()).contains("phin");
        assertThat(query.isSearch()).isTrue();
        assertThat(query.toQueryParams()).containsExactly(entry("query", "phin"), entry("limit", "10"));
    }

    @Test
    void limitMustBeBetweenOneAndAThousand() {
        assertThat(MemberQuery.MAX_LIMIT).isEqualTo(1000);
        assertThat(MemberQuery.list(1).limit()).isEqualTo(1);
        assertThat(MemberQuery.list(MemberQuery.MAX_LIMIT).limit()).isEqualTo(1000);

        assertThatThrownBy(() -> MemberQuery.list(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 1000");
        assertThatThrownBy(() -> MemberQuery.list(1001)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberQuery.search("phin", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isAValue() {
        assertThat(MemberQuery.list(10)).isEqualTo(MemberQuery.list(10));
        assertThat(MemberQuery.list(10)).hasSameHashCodeAs(MemberQuery.list(10));
        assertThat(MemberQuery.list(10)).isNotEqualTo(MemberQuery.search("a", 10));
    }
}
