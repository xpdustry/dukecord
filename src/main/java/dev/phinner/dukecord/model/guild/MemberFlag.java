// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import java.util.EnumSet;

public enum MemberFlag {
    DID_REJOIN(0),
    COMPLETED_ONBOARDING(1),
    BYPASSES_VERIFICATION(2),
    STARTED_ONBOARDING(3),
    IS_GUEST(4),
    STARTED_HOME_ACTIONS(5),
    COMPLETED_HOME_ACTIONS(6),
    AUTOMOD_QUARANTINED_USERNAME(7),
    DM_SETTINGS_UPSELL_ACKNOWLEDGED(9);

    private final int bit;

    MemberFlag(final int bit) {
        this.bit = bit;
    }

    public int bit() {
        return this.bit;
    }

    public static EnumSet<MemberFlag> fromBits(final int bits) {
        final var set = EnumSet.noneOf(MemberFlag.class);
        for (final var flag : values()) {
            if ((bits & (1 << flag.bit)) != 0) {
                set.add(flag);
            }
        }
        return set;
    }
}
