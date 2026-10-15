// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import dev.phinner.dukecord.Snowflake;
import java.util.List;

public interface RoleRestOperations {

    Role fetch(final Snowflake guildId, final Snowflake roleId);

    List<Role> fetchAllByGuild(final Snowflake guildId);

    Role create(final Snowflake guildId, final RoleSpec spec);

    Role create(final Snowflake guildId, final RoleSpec spec, final String reason);

    Role edit(final Snowflake guildId, final Snowflake roleId, final RoleSpec spec);

    Role edit(final Snowflake guildId, final Snowflake roleId, final RoleSpec spec, final String reason);

    void delete(final Snowflake guildId, final Snowflake roleId);

    void delete(final Snowflake guildId, final Snowflake roleId, final String reason);
}
