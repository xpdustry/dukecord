// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import dev.phinner.dukecord.Snowflake;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Needs the {@code GUILD_MEMBERS} intent. Discord may leave fields out of the update, such as {@code deaf} and
 * {@code mute}; they then hold their defaults here.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableGuildMemberUpdateEvent.class)
public interface GuildMemberUpdateEvent extends GuildEvent {

    @JsonUnwrapped
    Member member();

    @Override
    @Value.Derived
    default Snowflake guildId() {
        return this.member().guildId();
    }
}
