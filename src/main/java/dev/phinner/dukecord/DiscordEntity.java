// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

/** An immutable snapshot of a Discord object. */
public interface DiscordEntity {

    Snowflake id();
}
