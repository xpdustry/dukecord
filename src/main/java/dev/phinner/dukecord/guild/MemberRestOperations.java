// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Snowflake;
import java.util.List;

public interface MemberRestOperations {

    Member fetch(final Snowflake guildId, final Snowflake userId);

    /** Pages through every member. Needs the {@code GUILD_MEMBERS} intent. */
    List<Member> fetchAllByGuild(final Snowflake guildId);

    Member edit(final Snowflake guildId, final Snowflake userId, final MemberEditSpec spec);

    Member edit(final Snowflake guildId, final Snowflake userId, final MemberEditSpec spec, final String reason);

    void removeTimeout(final Snowflake guildId, final Snowflake userId);

    void removeTimeout(final Snowflake guildId, final Snowflake userId, final String reason);

    void addRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId);

    void addRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId, final String reason);

    void removeRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId);

    void removeRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId, final String reason);

    void kick(final Snowflake guildId, final Snowflake userId);

    void kick(final Snowflake guildId, final Snowflake userId, final String reason);
}
