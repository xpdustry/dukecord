// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import dev.phinner.dukecord.channel.ChannelRestOperations;
import dev.phinner.dukecord.guild.GuildRestOperations;
import dev.phinner.dukecord.message.MessageRestOperations;
import dev.phinner.dukecord.user.UserRestOperations;

/**
 * Blocking, typed access to Discord's REST API. Every method maps to one route and never reads or writes the cache.
 * Methods with a {@code reason} parameter record it in the guild audit log.
 */
public interface DiscordRest {

    GuildRestOperations guilds();

    ChannelRestOperations channels();

    MessageRestOperations messages();

    UserRestOperations users();
}
