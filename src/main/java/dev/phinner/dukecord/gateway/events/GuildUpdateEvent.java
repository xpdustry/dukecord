// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.gateway.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Guild;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableGuildUpdateEvent.class)
public interface GuildUpdateEvent extends GuildEvent {

    @JsonUnwrapped
    Guild guild();

    @Override
    @JsonIgnore
    @Value.Derived
    default Snowflake guildId() {
        return this.guild().id();
    }
}
