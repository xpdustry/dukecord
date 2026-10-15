// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.message.Message;
import dev.phinner.dukecord.message.MessageBehavior;
import dev.phinner.dukecord.message.MessageCreateSpec;
import java.util.Optional;

/** A channel known to accept messages, such as the channel of a message. */
public interface MessageChannelBehavior extends ChannelBehavior {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static MessageChannelBehavior of(@JacksonInject final Dukecord duke, final Snowflake id) {
        return new MessageChannelBehaviorImpl(duke, id);
    }

    @Override
    default Optional<MessageChannel> get() {
        return this.duke().channels().getById(this.id()).map(MessageChannelBehavior::asMessageChannel);
    }

    @Override
    default MessageChannel fetch() {
        return asMessageChannel(this.duke().channels().fetchById(this.id()));
    }

    @Override
    default MessageChannel fetch(final boolean force) {
        return asMessageChannel(this.duke().channels().fetchById(this.id(), force));
    }

    default MessageBehavior message(final Snowflake id) {
        return MessageBehavior.of(this.duke(), this.id(), id);
    }

    default Message send(final MessageCreateSpec spec) {
        return this.duke().messages().rest().create(this.id(), spec);
    }

    default Message send(final String content) {
        return this.send(MessageCreateSpec.of(content));
    }

    default ThreadChannel createThread(final String name) {
        return this.createThread(ThreadCreateSpec.of(name));
    }

    default ThreadChannel createThread(final ThreadCreateSpec spec) {
        return this.duke().channels().rest().createThread(this.id(), spec);
    }

    default ThreadChannel createThread(final ThreadCreateSpec spec, final String reason) {
        return this.duke().channels().rest().createThread(this.id(), spec, reason);
    }

    private static MessageChannel asMessageChannel(final Channel channel) {
        if (channel instanceof MessageChannel message) {
            return message;
        }
        throw new IllegalStateException("Channel " + channel.id() + " is not a message channel");
    }
}
