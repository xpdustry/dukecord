// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.DiscordEntityBehavior;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.channel.MessageChannelBehavior;
import dev.phinner.dukecord.channel.ThreadChannel;
import dev.phinner.dukecord.channel.ThreadCreateSpec;
import java.util.Optional;

public interface MessageBehavior extends DiscordEntityBehavior<Message> {

    static MessageBehavior of(final Dukecord duke, final Snowflake channelId, final Snowflake id) {
        return new MessageBehaviorImpl(duke, channelId, id);
    }

    Snowflake channelId();

    default MessageChannelBehavior channel() {
        return MessageChannelBehavior.of(this.duke(), this.channelId());
    }

    default Optional<Message> get() {
        return this.duke().messages().getByChannelAndId(this.channelId(), this.id());
    }

    @Override
    default Message fetch() {
        return this.duke().messages().fetchByChannelAndId(this.channelId(), this.id());
    }

    @Override
    default Message fetch(final boolean force) {
        return this.duke().messages().fetchByChannelAndId(this.channelId(), this.id(), force);
    }

    /** Mentions of the replied user are disabled unless the spec allows them. */
    default Message reply(final MessageCreateSpec spec) {
        return this.channel()
                .send(MessageCreateSpec.builder()
                        .from(spec)
                        .messageReference(MessageReference.of(this.channelId(), this.id()))
                        .build());
    }

    default Message reply(final String content) {
        return this.reply(MessageCreateSpec.of(content));
    }

    default Message edit(final MessageEditSpec spec) {
        return this.duke().messages().rest().edit(this.channelId(), this.id(), spec);
    }

    default Message edit(final String content) {
        return this.edit(MessageEditSpec.of(content));
    }

    default ThreadChannel createThread(final String name) {
        return this.createThread(ThreadCreateSpec.of(name));
    }

    default ThreadChannel createThread(final ThreadCreateSpec spec) {
        return this.duke().channels().rest().createThread(this.channelId(), this.id(), spec);
    }

    default ThreadChannel createThread(final ThreadCreateSpec spec, final String reason) {
        return this.duke().channels().rest().createThread(this.channelId(), this.id(), spec, reason);
    }

    default void delete() {
        this.duke().messages().rest().delete(this.channelId(), this.id());
    }

    default void delete(final String reason) {
        this.duke().messages().rest().delete(this.channelId(), this.id(), reason);
    }
}
