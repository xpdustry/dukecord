// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Snowflake;

public interface GuildRestOperations {

    Guild fetch(final Snowflake guildId);
}
