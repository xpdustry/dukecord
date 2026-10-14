// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.Snowflake;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableGuildUpdateEvent.class)
public interface GuildUpdateEvent extends GuildEvent {

    @JsonUnwrapped
    Guild guild();

    @Override
    @Value.Derived
    default Snowflake guildId() {
        return this.guild().id();
    }
}
