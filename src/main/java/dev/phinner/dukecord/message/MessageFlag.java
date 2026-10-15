// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.BitFlag;

/** A flag of a message. */
public enum MessageFlag implements BitFlag {
    /** Bits Dukecord does not know. */
    UNKNOWN(-1),
    /** Published to the channels following this one. */
    CROSSPOSTED(0),
    /** Copied from a channel this one follows. */
    IS_CROSSPOST(1),
    /** Hides link previews. Also valid on edits. */
    SUPPRESS_EMBEDS(2),
    /** The source of this crosspost was deleted. */
    SOURCE_MESSAGE_DELETED(3),
    /** Sent by the urgent message system. */
    URGENT(4),
    /** Has a thread with the same ID as the message. */
    HAS_THREAD(5),
    /** Only the user of the interaction sees the reply. Only valid on interaction replies and follow-ups. */
    EPHEMERAL(6),
    /** A deferred interaction reply, shown as "thinking". */
    LOADING(7),
    /** Some mentioned roles could not be added to the thread. */
    FAILED_TO_MENTION_SOME_ROLES_IN_THREAD(8),
    /** Sends without a push notification. */
    SUPPRESS_NOTIFICATIONS(12),
    /** A voice message. */
    IS_VOICE_MESSAGE(13),
    /** A forwarded message. */
    HAS_SNAPSHOT(14),
    /** Required for layout components other than action rows. Cannot be removed once set. */
    IS_COMPONENTS_V2(15);

    private final long mask;

    MessageFlag(final int bit) {
        this.mask = bit < 0 ? 0 : 1L << bit;
    }

    @Override
    public long mask() {
        return this.mask;
    }
}
