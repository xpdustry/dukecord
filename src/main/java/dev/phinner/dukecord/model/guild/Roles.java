// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRefList;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import java.util.Collection;

/** Builds {@link Role} snapshots and role reference lists. */
public final class Roles {

    /** Role objects carry no {@code guild_id}; the caller always knows the guild. */
    public static Role fromJson(final Dukecord duke, final Snowflake guildId, final Json json) {
        return new RoleImpl(duke, guildId, json);
    }

    /** A reference list resolving through the cache and fetching all roles of the guild in one request. */
    public static DiscordEntityRefList<Role> refs(
            final Dukecord duke, final Snowflake guildId, final Collection<Snowflake> ids) {
        return DiscordEntityRefList.of(
                Role.class,
                ids,
                id -> duke.cache().role(id).filter(role -> role.guild().id().equals(guildId)),
                id -> duke.rest().fetchRole(guildId, id),
                wanted -> duke.rest().fetchRoles(guildId).stream()
                        .filter(role -> wanted.contains(role.id()))
                        .toList());
    }

    private Roles() {}
}
