// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

/** How much of a guild's member set the cache knows, set by the code that knows (ready, chunking, intents). */
public enum MemberCompleteness {
    /** No member data at all, for example a guild fetched over REST. */
    NONE,
    /** Only the members Discord sent so far: the bot, voice participants, event authors, small-guild initial sets. */
    PARTIAL,
    /** Every member, after chunking or for a small guild with the members intent. */
    COMPLETE
}
