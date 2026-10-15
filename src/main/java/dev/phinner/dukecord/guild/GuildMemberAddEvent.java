// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.Snowflake;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Needs the {@code GUILD_MEMBERS} intent. */
@Value.Immutable
@JsonDeserialize(as = ImmutableGuildMemberAddEvent.class)
public interface GuildMemberAddEvent extends GuildEvent {

    @JsonUnwrapped
    Member member();

    @Override
    @Value.Derived
    default Snowflake guildId() {
        return this.member().guildId();
    }
}
