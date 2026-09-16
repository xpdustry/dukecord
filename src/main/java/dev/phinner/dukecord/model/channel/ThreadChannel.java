// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.Snowflake;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableThreadChannel.class)
public non-sealed interface ThreadChannel extends GuildChannel, MessageChannel {

    Snowflake ownerId();

    @Value.Default
    default int messageCount() {
        return 0;
    }

    @Value.Default
    default int memberCount() {
        return 0;
    }
}
