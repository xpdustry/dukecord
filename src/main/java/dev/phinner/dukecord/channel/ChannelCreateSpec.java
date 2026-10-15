// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.channel;

import dev.phinner.dukecord.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * A guild channel to create. The kind of channel comes from the method it is passed to, such as
 * {@link dev.phinner.dukecord.guild.GuildBehavior#createTextChannel(ChannelCreateSpec)}. Categories ignore the text channel
 * attributes.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableChannelCreateSpec.class)
public interface ChannelCreateSpec {

    static Builder builder() {
        return ImmutableChannelCreateSpec.builder();
    }

    static ChannelCreateSpec of(final String name) {
        return ImmutableChannelCreateSpec.of(name);
    }

    /** 1 to 100 characters. */
    @Value.Parameter
    String name();

    /** The category to put the channel in. */
    Optional<Snowflake> parentId();

    Optional<Integer> position();

    /** 0 to 1024 characters. */
    Optional<String> topic();

    Optional<Boolean> nsfw();

    /** Seconds a user waits between messages, 0 to 21600. */
    Optional<Integer> rateLimitPerUser();

    interface Builder {
        Builder from(final ChannelCreateSpec instance);

        Builder name(final String name);

        Builder parentId(final Snowflake parentId);

        Builder position(final int position);

        Builder topic(final String topic);

        Builder nsfw(final boolean nsfw);

        Builder rateLimitPerUser(final int rateLimitPerUser);

        ChannelCreateSpec build();
    }
}
