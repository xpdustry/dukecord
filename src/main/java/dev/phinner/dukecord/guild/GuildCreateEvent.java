// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.Snowflake;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableGuildCreateEvent.class)
public interface GuildCreateEvent extends GuildEvent, GuildCreatePayload {

    @JsonUnwrapped
    Guild guild();

    @Override
    @Value.Derived
    default Snowflake guildId() {
        return this.guild().id();
    }
}
