// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.gateway.DiscordGatewayEvent;

public interface GuildEvent extends DiscordGatewayEvent {

    Snowflake guildId();
}
