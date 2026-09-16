// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.message.Message;
import dev.phinner.dukecord.model.message.MessageCreateSpec;
import dev.phinner.dukecord.model.message.MessageEditSpec;
import dev.phinner.dukecord.model.message.MessageQuery;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** A channel messages can be sent to. */
public sealed interface MessageChannel extends Channel permits GuildTextChannel, ThreadChannel, DmChannel {

    /** Kept current from {@code MESSAGE_CREATE}, since Discord emits no channel update for it. */
    Optional<DiscordEntityRef<Message>> lastMessage();

    default Message send(final MessageCreateSpec spec) {
        return this.duke().rest().sendMessage(this.id(), spec);
    }

    default Message send(final String content) {
        return this.send(MessageCreateSpec.builder().content(content).build());
    }

    default Message fetchMessage(final Snowflake messageId) {
        return this.duke().rest().fetchMessage(this.id(), messageId);
    }

    default List<Message> fetchMessages(final MessageQuery query) {
        return this.duke().rest().fetchMessages(this.id(), query);
    }

    default Message editMessage(final Snowflake messageId, final MessageEditSpec spec) {
        return this.duke().rest().editMessage(this.id(), messageId, spec);
    }

    default void bulkDelete(final Collection<Snowflake> messageIds) {
        this.duke().rest().bulkDeleteMessages(this.id(), messageIds);
    }

    default ThreadChannel createThread(final ThreadCreateSpec spec) {
        return this.duke().rest().createThread(this.id(), spec);
    }

    default void typing() {
        this.duke().rest().triggerTyping(this.id());
    }
}
