// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.phinner.dukecord.message.MessageBehavior;
import java.util.Optional;

public sealed interface MessageChannel extends Channel, MessageChannelBehavior
        permits DmChannel, GuildTextChannel, ThreadChannel {

    /** The message may have been deleted since. Decoded by {@link MessageChannelDeserializer}. */
    @JsonIgnore
    Optional<MessageBehavior> lastMessage();
}
