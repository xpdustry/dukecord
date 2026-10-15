// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** An emoji uploaded to a guild or an application. */
@Value.Immutable
@JsonDeserialize(as = ImmutableCustomEmoji.class)
public non-sealed interface CustomEmoji extends Emoji, DiscordEntity {

    static Builder builder() {
        return ImmutableCustomEmoji.builder();
    }

    @Override
    @Value.Parameter
    Snowflake id();

    /** Absent when the emoji was deleted. */
    @Value.Parameter
    Optional<String> name();

    @Value.Default
    @Value.Parameter
    default boolean animated() {
        return false;
    }

    interface Builder {
        Builder from(final CustomEmoji instance);

        Builder id(final Snowflake id);

        Builder name(final String name);

        Builder animated(final boolean animated);

        CustomEmoji build();
    }
}
