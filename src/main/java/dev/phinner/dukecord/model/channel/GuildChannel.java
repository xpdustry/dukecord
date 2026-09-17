// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.store.DiscordEntityRef;
import java.util.Optional;
import org.immutables.value.Value;

public sealed interface GuildChannel extends Channel permits CategoryChannel, GuildTextChannel, ThreadChannel {

    @JsonProperty("guild_id")
    DiscordEntityRef<Guild> guild();

    @Value.Default
    default String name() {
        return "";
    }

    @Value.Default
    default int position() {
        return 0;
    }

    @JsonProperty("parent_id")
    Optional<DiscordEntityRef<Channel>> parent();

    @Value.Default
    default int flags() {
        return 0;
    }
}
