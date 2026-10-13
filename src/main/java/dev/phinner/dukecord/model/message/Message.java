// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.Channel;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.user.User;
import dev.phinner.dukecord.store.DiscordEntityRef;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableMessage.class)
public interface Message extends DiscordEntity {

    @JsonProperty("channel_id")
    DiscordEntityRef<Channel> channel();

    @JsonProperty("guild_id")
    Optional<DiscordEntityRef<Guild>> guild();

    User author();

    Optional<Snowflake> webhookId();

    @Value.Default
    default String content() {
        return "";
    }

    Instant timestamp();

    Optional<Instant> editedTimestamp();

    @Value.Default
    default int type() {
        return 0;
    }

    @Value.Default
    default int flags() {
        return 0;
    }

    @Value.Default
    default boolean tts() {
        return false;
    }

    @Value.Default
    default boolean pinned() {
        return false;
    }

    List<User> mentions();
}
