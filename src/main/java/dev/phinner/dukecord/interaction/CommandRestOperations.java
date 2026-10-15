// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.Snowflake;
import java.util.List;

/** Global commands reach every guild and DM; guild commands update instantly, which suits development. */
public interface CommandRestOperations {

    List<ApplicationCommand> fetchGlobal();

    /** Replaces every global command with these. */
    List<ApplicationCommand> overwriteGlobal(final List<CommandSpec> specs);

    ApplicationCommand createGlobal(final CommandSpec spec);

    void deleteGlobal(final Snowflake commandId);

    List<ApplicationCommand> fetchGuild(final Snowflake guildId);

    /** Replaces every command of the guild with these. */
    List<ApplicationCommand> overwriteGuild(final Snowflake guildId, final List<CommandSpec> specs);

    ApplicationCommand createGuild(final Snowflake guildId, final CommandSpec spec);

    void deleteGuild(final Snowflake guildId, final Snowflake commandId);
}
