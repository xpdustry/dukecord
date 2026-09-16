// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import dev.phinner.dukecord.Dukecord;

/**
 * An immutable snapshot of a Discord object. Entities never change after construction; the cache replaces whole
 * values. Two snapshots are equal when their data is equal, whatever their origin.
 */
public interface DiscordEntity {

    Dukecord duke();

    Snowflake id();

    /** The frozen source object, the escape hatch for fields that are not modelled. */
    Json raw();
}
