// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import dev.phinner.dukecord.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageReference.class)
public interface MessageReference {

    static MessageReference of(final Snowflake channelId, final Snowflake messageId) {
        return ImmutableMessageReference.builder()
                .channelId(channelId)
                .messageId(messageId)
                .build();
    }

    Snowflake messageId();

    Optional<Snowflake> channelId();

    Optional<Boolean> failIfNotExists();
}
