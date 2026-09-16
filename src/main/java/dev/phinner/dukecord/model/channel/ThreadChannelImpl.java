// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.guild.Members;
import dev.phinner.dukecord.model.message.Message;
import java.util.Optional;

final class ThreadChannelImpl extends AbstractGuildChannel implements ThreadChannel {

    private final DiscordEntityRef<GuildChannel> parent;
    private final DiscordEntityRef<Member> owner;
    private final ThreadMetadata metadata;
    private final int messageCount;
    private final int memberCount;
    private final boolean joined;
    private final Optional<DiscordEntityRef<Message>> lastMessage;

    ThreadChannelImpl(final Dukecord duke, final Json raw, final boolean joined) {
        super(duke, raw);
        this.parent = Channels.ref(duke, GuildChannel.class, raw.snowflake("parent_id"));
        this.owner = Members.ref(duke, this.guildId(), raw.snowflake("owner_id"));
        this.metadata = raw.optionalObject("thread_metadata")
                .map(ThreadMetadata::fromJson)
                .orElseGet(() -> ThreadMetadata.fromJson(Json.empty()));
        this.messageCount = raw.optionalInteger("message_count").orElse(0);
        this.memberCount = raw.optionalInteger("member_count").orElse(0);
        this.joined = joined;
        this.lastMessage = this.lastMessageRef();
    }

    @Override
    public DiscordEntityRef<GuildChannel> parent() {
        return this.parent;
    }

    @Override
    public DiscordEntityRef<Member> owner() {
        return this.owner;
    }

    @Override
    public ThreadMetadata metadata() {
        return this.metadata;
    }

    @Override
    public int messageCount() {
        return this.messageCount;
    }

    @Override
    public int memberCount() {
        return this.memberCount;
    }

    @Override
    public boolean isJoined() {
        return this.joined;
    }

    @Override
    public Optional<DiscordEntityRef<Message>> lastMessage() {
        return this.lastMessage;
    }
}
