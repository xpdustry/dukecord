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

    static MessageReference of(final Snowflake channelId, final Snowflake messageId) {
        return ImmutableMessageReference.builder()
                .channelId(channelId)
                .messageId(messageId)
                .build();
    }

    /** 0 for a reply or crosspost, 1 for a forward. */
    @Value.Default
    default int type() {
        return 0;
    }

    Snowflake messageId();

    Optional<Snowflake> channelId();

    Optional<Snowflake> guildId();

    Optional<Boolean> failIfNotExists();
}
