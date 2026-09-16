// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.cache;

import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channel;
import dev.phinner.dukecord.model.channel.GuildChannel;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.Role;
import dev.phinner.dukecord.model.message.Message;
import dev.phinner.dukecord.model.user.User;
import java.util.List;
import java.util.Optional;

/**
 * Read side of the cache. Every method is a lock-free lookup returning an immutable snapshot or an immutable list
 * copy; there are no live views and no I/O. Only the gateway writes.
 */
public interface DiscordCache {

    Optional<Guild> guild(final Snowflake id);

    List<Guild> guilds();

    Optional<Channel> channel(final Snowflake id);

    default <C> Optional<C> channel(final Snowflake id, final Class<C> type) {
        return this.channel(id).filter(type::isInstance).map(type::cast);
    }

    Optional<Role> role(final Snowflake id);

    Optional<Member> member(final Snowflake guildId, final Snowflake userId);

    Optional<User> user(final Snowflake id);

    Optional<Message> message(final Snowflake channelId, final Snowflake messageId);

    /** The cached channels of a guild, including threads. */
    List<GuildChannel> channels(final Snowflake guildId);

    List<Role> roles(final Snowflake guildId);

    /** The known members of a guild; see {@link Guild#memberCompleteness()} for what "known" means. */
    List<Member> members(final Snowflake guildId);
}
