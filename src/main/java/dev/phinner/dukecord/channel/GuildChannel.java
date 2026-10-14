// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.guild.GuildBehavior;
import java.util.Optional;
import org.immutables.value.Value;

public sealed interface GuildChannel extends Channel permits CategoryChannel, GuildTextChannel, ThreadChannel {

    @JsonProperty("guild_id")
    GuildBehavior guild();

    @Value.Default
    default String name() {
        return "";
    }

    @Value.Default
    default int position() {
        return 0;
    }

    @JsonProperty("parent_id")
    Optional<ChannelBehavior> parent();

    @Value.Default
    default int flags() {
        return 0;
    }
}
