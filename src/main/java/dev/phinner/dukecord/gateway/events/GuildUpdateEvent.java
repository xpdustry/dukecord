// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Guild;

public record GuildUpdateEvent(Guild guild) implements GuildEvent {

    @Override
    public Snowflake guildId() {
        return this.guild.id();
    }
}
