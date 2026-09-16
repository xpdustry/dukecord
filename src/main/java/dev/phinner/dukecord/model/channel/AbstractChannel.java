// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.AbstractDiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.message.Message;
import java.util.Optional;

/** Not itself a {@link Channel}: the sealed hierarchy admits only the concrete leaf implementations. */
abstract class AbstractChannel extends AbstractDiscordEntity {

    private final int typeCode;
    private final ChannelType type;

    AbstractChannel(final Dukecord duke, final Json raw) {
        super(duke, raw);
        this.typeCode = raw.integer("type");
        this.type = ChannelType.fromCode(this.typeCode);
    }

    public final ChannelType type() {
        return this.type;
    }

    public final int typeCode() {
        return this.typeCode;
    }

    /** Shared by every {@link MessageChannel} implementation. */
    final Optional<DiscordEntityRef<Message>> lastMessageRef() {
        final Snowflake channelId = this.id();
        return this.raw()
                .optionalSnowflake("last_message_id")
                .map(messageId -> DiscordEntityRef.of(
                        Message.class, messageId, id -> this.duke().cache().message(channelId, id), id -> this.duke()
                                .rest()
                                .fetchMessage(channelId, id)));
    }
}
