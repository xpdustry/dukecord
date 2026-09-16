// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import java.util.EnumSet;

public enum MessageFlag {
    CROSSPOSTED(0),
    IS_CROSSPOST(1),
    SUPPRESS_EMBEDS(2),
    SOURCE_MESSAGE_DELETED(3),
    URGENT(4),
    HAS_THREAD(5),
    EPHEMERAL(6),
    LOADING(7),
    FAILED_TO_MENTION_SOME_ROLES_IN_THREAD(8),
    SUPPRESS_NOTIFICATIONS(12),
    IS_VOICE_MESSAGE(13),
    HAS_SNAPSHOT(14),
    /** Irreversible once set: the message can no longer carry content or embeds. */
    IS_COMPONENTS_V2(15);

    private final int bit;

    MessageFlag(final int bit) {
        this.bit = bit;
    }

    public int bit() {
        return this.bit;
    }

    public int mask() {
        return 1 << this.bit;
    }

    public static EnumSet<MessageFlag> fromBits(final int bits) {
        final var set = EnumSet.noneOf(MessageFlag.class);
        for (final var flag : values()) {
            if ((bits & flag.mask()) != 0) {
                set.add(flag);
            }
        }
        return set;
    }

    public static int toBits(final Iterable<MessageFlag> flags) {
        int bits = 0;
        for (final var flag : flags) {
            bits |= flag.mask();
        }
        return bits;
    }
}
