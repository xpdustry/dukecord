// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

/**
 * A flag of a Discord bit field. A {@code Set} of flags is sent as the integer of their bits. Each flag enum has an
 * {@code UNKNOWN} entry whose mask is {@code 0}, which a decoded set holds when it had bits Dukecord does not know.
 * {@code UNKNOWN} is never sent.
 */
public interface BitFlag {

    long mask();
}
