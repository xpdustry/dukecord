// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.store.DiscordEntityRef;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableThreadChannel.class)
public non-sealed interface ThreadChannel extends GuildChannel, MessageChannel {

    @JsonProperty("owner_id")
    DiscordEntityRef<User> owner();

    @Value.Default
    default int messageCount() {
        return 0;
    }

    @Value.Default
    default int memberCount() {
        return 0;
    }
}
