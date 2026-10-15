// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.BitFlag;

/** A flag of a guild member. */
public enum MemberFlag implements BitFlag {
    /** Bits Dukecord does not know. */
    UNKNOWN(-1),
    /** Left and rejoined the guild. */
    DID_REJOIN(0),
    COMPLETED_ONBOARDING(1),
    /** Exempt from the verification requirements. The only flag a bot can change. */
    BYPASSES_VERIFICATION(2),
    STARTED_ONBOARDING(3),
    /** Can only access the voice channel they were invited to. */
    IS_GUEST(4),
    /** Started the new member actions of the server guide. */
    STARTED_HOME_ACTIONS(5),
    /** Completed the new member actions of the server guide. */
    COMPLETED_HOME_ACTIONS(6),
    /** AutoMod blocked their username, display name or nickname. */
    AUTOMOD_QUARANTINED_USERNAME(7),
    /** Dismissed the DM settings upsell. */
    DM_SETTINGS_UPSELL_ACKNOWLEDGED(9),
    /** AutoMod blocked their guild tag. */
    AUTOMOD_QUARANTINED_GUILD_TAG(10);

    private final long mask;

    MemberFlag(final int bit) {
        this.mask = bit < 0 ? 0 : 1L << bit;
    }

    @Override
    public long mask() {
        return this.mask;
    }
}
