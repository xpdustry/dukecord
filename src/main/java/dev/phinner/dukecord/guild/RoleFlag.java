// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.BitFlag;

/** A flag of a role. */
public enum RoleFlag implements BitFlag {
    /** Bits Dukecord does not know. */
    UNKNOWN(-1),
    /** Members can pick it in an onboarding prompt. */
    IN_PROMPT(0);

    private final long mask;

    RoleFlag(final int bit) {
        this.mask = bit < 0 ? 0 : 1L << bit;
    }

    @Override
    public long mask() {
        return this.mask;
    }
}
