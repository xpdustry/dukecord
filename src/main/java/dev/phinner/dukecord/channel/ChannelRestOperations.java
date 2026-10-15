// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import dev.phinner.dukecord.Snowflake;
import java.util.List;

public interface ChannelRestOperations {

    Channel fetch(final Snowflake channelId);

    /** Threads are not included. */
    List<GuildChannel> fetchAllByGuild(final Snowflake guildId);

    /** Returns the existing DM channel with the user if there is one. */
    DmChannel createDm(final Snowflake recipientId);

    void delete(final Snowflake channelId);

    void delete(final Snowflake channelId, final String reason);

    GuildTextChannel createTextChannel(final Snowflake guildId, final ChannelCreateSpec spec);

    GuildTextChannel createTextChannel(final Snowflake guildId, final ChannelCreateSpec spec, final String reason);

    CategoryChannel createCategory(final Snowflake guildId, final ChannelCreateSpec spec);

    CategoryChannel createCategory(final Snowflake guildId, final ChannelCreateSpec spec, final String reason);

    /** Starts a thread on its own. The bot joins the threads it creates. */
    ThreadChannel createThread(final Snowflake channelId, final ThreadCreateSpec spec);

    ThreadChannel createThread(final Snowflake channelId, final ThreadCreateSpec spec, final String reason);

    /** Starts a thread from a message, sharing the message's ID. */
    ThreadChannel createThread(final Snowflake channelId, final Snowflake messageId, final ThreadCreateSpec spec);

    ThreadChannel createThread(
            final Snowflake channelId, final Snowflake messageId, final ThreadCreateSpec spec, final String reason);

    void joinThread(final Snowflake threadId);

    void leaveThread(final Snowflake threadId);
}
