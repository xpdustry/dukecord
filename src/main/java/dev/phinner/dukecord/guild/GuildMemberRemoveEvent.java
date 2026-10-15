// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.user.User;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Needs the {@code GUILD_MEMBERS} intent. Sent for leaves, kicks and bans. */
@Value.Immutable
@JsonDeserialize(as = ImmutableGuildMemberRemoveEvent.class)
public interface GuildMemberRemoveEvent extends GuildEvent {

    User user();
}
