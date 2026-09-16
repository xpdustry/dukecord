// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.rest;

import dev.phinner.dukecord.http.DiscordHttpClient;
import dev.phinner.dukecord.model.Emoji;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.CategoryChannel;
import dev.phinner.dukecord.model.channel.Channel;
import dev.phinner.dukecord.model.channel.ChannelCreateSpec;
import dev.phinner.dukecord.model.channel.DmChannel;
import dev.phinner.dukecord.model.channel.GuildChannel;
import dev.phinner.dukecord.model.channel.GuildTextChannel;
import dev.phinner.dukecord.model.channel.ThreadChannel;
import dev.phinner.dukecord.model.channel.ThreadCreateSpec;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.MemberEditSpec;
import dev.phinner.dukecord.model.guild.MemberQuery;
import dev.phinner.dukecord.model.guild.Role;
import dev.phinner.dukecord.model.message.Attachment;
import dev.phinner.dukecord.model.message.Message;
import dev.phinner.dukecord.model.message.MessageCreateSpec;
import dev.phinner.dukecord.model.message.MessageEditSpec;
import dev.phinner.dukecord.model.message.MessageQuery;
import dev.phinner.dukecord.model.user.User;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Blocking, typed REST facade. Every method maps to one route, returns entities built with the same factories as the
 * gateway path, and never writes to the cache. Failures throw {@link DiscordRestException}; 404 throws
 * {@link DiscordNotFoundException}, 403 throws {@link DiscordForbiddenException}.
 */
public interface DiscordRest {

    User fetchSelf();

    User fetchUser(final Snowflake id);

    Guild fetchGuild(final Snowflake id);

    Channel fetchChannel(final Snowflake id);

    Member fetchMember(final Snowflake guildId, final Snowflake userId);

    List<Member> fetchMembers(final Snowflake guildId, final MemberQuery query);

    List<Role> fetchRoles(final Snowflake guildId);

    Role fetchRole(final Snowflake guildId, final Snowflake roleId);

    List<ThreadChannel> fetchActiveThreads(final Snowflake guildId);

    Message fetchMessage(final Snowflake channelId, final Snowflake id);

    List<Message> fetchMessages(final Snowflake channelId, final MessageQuery query);

    Message sendMessage(final Snowflake channelId, final MessageCreateSpec spec);

    Message editMessage(final Snowflake channelId, final Snowflake id, final MessageEditSpec spec);

    void deleteMessage(final Snowflake channelId, final Snowflake id);

    void deleteMessage(final Snowflake channelId, final Snowflake id, final String reason);

    /** Deletes 2 to 100 messages, none older than 14 days. */
    void bulkDeleteMessages(final Snowflake channelId, final Collection<Snowflake> ids);

    void pinMessage(final Snowflake channelId, final Snowflake id);

    void unpinMessage(final Snowflake channelId, final Snowflake id);

    void addReaction(final Snowflake channelId, final Snowflake messageId, final Emoji emoji);

    void triggerTyping(final Snowflake channelId);

    /** Starts a thread without a message (text channels) or with a starter message (forum and media channels). */
    ThreadChannel createThread(final Snowflake channelId, final ThreadCreateSpec spec);

    /** Starts a thread anchored on an existing message. */
    ThreadChannel createThread(final Snowflake channelId, final Snowflake messageId, final ThreadCreateSpec spec);

    void joinThread(final Snowflake threadId);

    void leaveThread(final Snowflake threadId);

    GuildChannel createChannel(final Snowflake guildId, final ChannelCreateSpec spec);

    GuildTextChannel createTextChannel(final Snowflake guildId, final ChannelCreateSpec spec);

    CategoryChannel createCategory(final Snowflake guildId, final ChannelCreateSpec spec);

    void deleteChannel(final Snowflake channelId, final String reason);

    Member editMember(final Snowflake guildId, final Snowflake userId, final MemberEditSpec spec);

    void addMemberRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId);

    void addMemberRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId, final String reason);

    void removeMemberRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId);

    void removeMemberRole(final Snowflake guildId, final Snowflake userId, final Snowflake roleId, final String reason);

    void kickMember(final Snowflake guildId, final Snowflake userId, final String reason);

    DmChannel openDm(final Snowflake userId);

    /** Downloads an attachment from the CDN. The caller closes the stream. */
    InputStream download(final Attachment attachment);

    /** The raw HTTP client, for routes this facade does not cover. */
    DiscordHttpClient http();
}
