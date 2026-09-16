// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableGuildTextChannel.class)
public non-sealed interface GuildTextChannel extends GuildChannel, MessageChannel {

    Optional<String> topic();

    @Value.Default
    default boolean nsfw() {
        return false;
    }

    @Value.Default
    default int rateLimitPerUser() {
        return 0;
    }
}
