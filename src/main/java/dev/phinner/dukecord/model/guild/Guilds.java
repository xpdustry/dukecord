// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;

/** Builds {@link Guild} snapshots. The snapshot holds scalars only; children go to their own cache stores. */
public final class Guilds {

    /** A guild fetched over REST or seen in an event: available unless the payload says otherwise, no member data. */
    public static Guild fromJson(final Dukecord duke, final Json json) {
        return fromJson(duke, json, !json.bool("unavailable", false), MemberCompleteness.NONE);
    }

    /** The cache's constructor: availability and completeness are set by the reducer that knows. */
    public static Guild fromJson(
            final Dukecord duke, final Json json, final boolean available, final MemberCompleteness completeness) {
        return new GuildImpl(duke, json, available, completeness);
    }

    public static DiscordEntityRef<Guild> ref(final Dukecord duke, final Snowflake guildId) {
        return DiscordEntityRef.of(Guild.class, guildId, duke.cache()::guild, duke.rest()::fetchGuild);
    }

    private Guilds() {}
}
