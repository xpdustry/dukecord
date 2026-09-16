// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.model.user.Users;

/** Builds {@link Member} snapshots. Member objects carry no {@code guild_id}; the caller always knows the guild. */
public final class Members {

    /** Requires a full {@code user} object inside the payload. */
    public static Member fromJson(final Dukecord duke, final Snowflake guildId, final Json json) {
        return new MemberImpl(duke, guildId, json, Users.fromJson(duke, json.object("user")));
    }

    /**
     * For payloads whose member lacks {@code user} ({@code MESSAGE_CREATE.member}, {@code mentions[].member}): the
     * user is merged into the raw object so the snapshot is complete.
     */
    public static Member fromJson(final Dukecord duke, final Snowflake guildId, final Json json, final User user) {
        return new MemberImpl(duke, guildId, json.with("user", user.raw()), user);
    }

    public static DiscordEntityRef<Member> ref(final Dukecord duke, final Snowflake guildId, final Snowflake userId) {
        return DiscordEntityRef.of(Member.class, userId, id -> duke.cache().member(guildId, id), id -> duke.rest()
                .fetchMember(guildId, id));
    }

    private Members() {}
}
