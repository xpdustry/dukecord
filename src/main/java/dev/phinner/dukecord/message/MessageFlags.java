// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

/** The message flags a bot can set. */
public final class MessageFlags {

    /** Hides link previews. Also valid on edits. */
    public static final int SUPPRESS_EMBEDS = 1 << 2;

    /** Only the user of the interaction sees the reply. Only valid on interaction replies and follow-ups. */
    public static final int EPHEMERAL = 1 << 6;

    /** Sends without a push notification. */
    public static final int SUPPRESS_NOTIFICATIONS = 1 << 12;

    /** Required for layout components other than action rows. Cannot be removed once set. */
    public static final int IS_COMPONENTS_V2 = 1 << 15;

    private MessageFlags() {}
}
