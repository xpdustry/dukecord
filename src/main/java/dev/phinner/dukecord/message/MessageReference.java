// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Points at another message, for replies, forwards and crossposts. */
@Value.Immutable
@JsonDeserialize(as = ImmutableMessageReference.class)
public interface MessageReference {

    static Builder builder() {
        return ImmutableMessageReference.builder();
    }

    static MessageReference of(final Snowflake channelId, final Snowflake messageId) {
        return ImmutableMessageReference.of(messageId, Optional.of(channelId));
    }

    /** 0 for a reply or crosspost, 1 for a forward. */
    @Value.Default
    default int type() {
        return 0;
    }

    @Value.Parameter
    Snowflake messageId();

    @Value.Parameter
    Optional<Snowflake> channelId();

    Optional<Snowflake> guildId();

    Optional<Boolean> failIfNotExists();

    interface Builder {
        Builder from(final MessageReference instance);

        Builder type(final int type);

        Builder messageId(final Snowflake messageId);

        Builder channelId(final Snowflake channelId);

        Builder guildId(final Snowflake guildId);

        Builder failIfNotExists(final boolean failIfNotExists);

        MessageReference build();
    }
}
