// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.permission;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionSetTest {

    @Test
    void bitsMatchDiscordsTable() {
        assertThat(Permission.ADMINISTRATOR.bit()).isEqualTo(3);
        assertThat(Permission.VIEW_CHANNEL.bit()).isEqualTo(10);
        assertThat(Permission.SEND_MESSAGES.bit()).isEqualTo(11);
        assertThat(Permission.PIN_MESSAGES.bit()).isEqualTo(51);
        assertThat(Permission.CREATE_INSTANT_INVITE.bit()).isZero();
    }

    @Test
    void masksAreTheShiftedBits() {
        assertThat(Permission.CREATE_INSTANT_INVITE.mask()).isEqualTo(1L);
        assertThat(Permission.ADMINISTRATOR.mask()).isEqualTo(8L);
        assertThat(Permission.VIEW_CHANNEL.mask()).isEqualTo(1024L);
        assertThat(Permission.PIN_MESSAGES.mask()).isEqualTo(1L << 51);
    }

    @Test
    void everyPermissionHasAUniqueBitBelowSixtyTwo() {
        long seen = 0L;
        for (final var permission : Permission.values()) {
            assertThat(permission.bit()).isBetween(0, 61);
            assertThat(seen & permission.mask())
                    .describedAs("duplicate bit %s", permission)
                    .isZero();
            seen |= permission.mask();
        }
    }

    @Test
    void parsesAndWritesDiscordsStringForm() {
        assertThat(PermissionSet.fromString("1024")).isEqualTo(PermissionSet.of(Permission.VIEW_CHANNEL));
        assertThat(PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES)
                        .asString())
                .isEqualTo("3072");
        assertThat(PermissionSet.NONE.asString()).isEqualTo("0");
        assertThat(PermissionSet.fromString(PermissionSet.ALL.asString())).isEqualTo(PermissionSet.ALL);
    }

    @Test
    void stringFormIsUnsignedSoBitSixtyThreeSurvives() {
        final var high = new PermissionSet(1L << 63);
        assertThat(high.asString()).isEqualTo("9223372036854775808");
        assertThat(PermissionSet.fromString("9223372036854775808")).isEqualTo(high);
    }

    @Test
    void ofBuildsFromVarargsOrCollections() {
        final var expected = PermissionSet.of(Permission.KICK_MEMBERS, Permission.BAN_MEMBERS);
        assertThat(PermissionSet.of(List.of(Permission.BAN_MEMBERS, Permission.KICK_MEMBERS)))
                .isEqualTo(expected);
        assertThat(PermissionSet.of()).isEqualTo(PermissionSet.NONE);
        assertThat(PermissionSet.of(List.of())).isEqualTo(PermissionSet.NONE);
    }

    @Test
    void containsTestsASingleBit() {
        final var set = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES);
        assertThat(set.contains(Permission.VIEW_CHANNEL)).isTrue();
        assertThat(set.contains(Permission.MANAGE_MESSAGES)).isFalse();
        assertThat(PermissionSet.NONE.isEmpty()).isTrue();
        assertThat(set.isEmpty()).isFalse();
    }

    @Test
    void containsAllAndContainsAnyDifferOnPartialOverlap() {
        final var set = PermissionSet.of(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES);
        assertThat(set.containsAll(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES))
                .isTrue();
        assertThat(set.containsAll(Permission.VIEW_CHANNEL, Permission.BAN_MEMBERS))
                .isFalse();
        assertThat(set.containsAll(PermissionSet.NONE)).isTrue();
        assertThat(set.containsAny(PermissionSet.of(Permission.VIEW_CHANNEL, Permission.BAN_MEMBERS)))
                .isTrue();
        assertThat(set.containsAny(PermissionSet.of(Permission.BAN_MEMBERS))).isFalse();
        assertThat(set.containsAny(PermissionSet.NONE)).isFalse();
    }

    @Test
    void withAndWithoutReturnCopies() {
        final var base = PermissionSet.of(Permission.VIEW_CHANNEL);
        final var extended = base.with(Permission.SEND_MESSAGES);
        assertThat(base).isEqualTo(PermissionSet.of(Permission.VIEW_CHANNEL));
        assertThat(extended.asSet()).containsExactly(Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES);
        assertThat(extended.without(Permission.SEND_MESSAGES)).isEqualTo(base);
        assertThat(extended.without(Permission.BAN_MEMBERS)).isEqualTo(extended);
        assertThat(base.with(PermissionSet.of(Permission.SEND_MESSAGES))).isEqualTo(extended);
        assertThat(extended.without(PermissionSet.of(Permission.SEND_MESSAGES, Permission.BAN_MEMBERS)))
                .isEqualTo(base);
    }

    @Test
    void asSetIsOrderedByBitAndDetached() {
        final var set = PermissionSet.of(Permission.PIN_MESSAGES, Permission.ADMINISTRATOR, Permission.VIEW_CHANNEL);
        assertThat(set.asSet())
                .containsExactly(Permission.ADMINISTRATOR, Permission.VIEW_CHANNEL, Permission.PIN_MESSAGES);
        final var copy = set.asSet();
        copy.clear();
        assertThat(set.asSet()).hasSize(3);
        assertThat(PermissionSet.NONE.asSet()).isEmpty();
    }

    @Test
    void allContainsEveryKnownPermission() {
        assertThat(PermissionSet.ALL.asSet()).isEqualTo(EnumSet.allOf(Permission.class));
        for (final var permission : Permission.values()) {
            assertThat(PermissionSet.ALL.contains(permission))
                    .describedAs("%s", permission)
                    .isTrue();
        }
        assertThat(PermissionSet.ALL.containsAll(PermissionSet.ALL)).isTrue();
        assertThat(PermissionSet.ALL.containsAll(PermissionSet.of(Permission.PIN_MESSAGES)))
                .isTrue();
        assertThat(PermissionSet.NONE.containsAll(PermissionSet.ALL)).isFalse();
    }

    @Test
    void toStringListsTheNames() {
        assertThat(PermissionSet.of(Permission.VIEW_CHANNEL)).hasToString("[VIEW_CHANNEL]");
        assertThat(PermissionSet.NONE).hasToString("[]");
    }
}
