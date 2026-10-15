// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.DiscordEntity;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.channel.MessageChannelBehavior;
import dev.phinner.dukecord.component.Component;
import dev.phinner.dukecord.guild.GuildBehavior;
import dev.phinner.dukecord.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableMessage.class)
public interface Message extends DiscordEntity, MessageBehavior {

    @Override
    @JsonProperty("channel_id")
    MessageChannelBehavior channel();

    @Override
    default Snowflake channelId() {
        return this.channel().id();
    }

    @JsonProperty("guild_id")
    Optional<GuildBehavior> guild();

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

    List<Attachment> attachments();

    List<Embed> embeds();

    List<Component> components();

    /** Set on replies, forwards and crossposts. */
    Optional<MessageReference> messageReference();
}
