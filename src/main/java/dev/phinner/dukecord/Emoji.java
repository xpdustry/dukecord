// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A unicode emoji, or a custom one uploaded to a guild or the application. */
@Value.Immutable
@JsonDeserialize(as = ImmutableEmoji.class)
public interface Emoji {

    static Emoji unicode(final String emoji) {
        return ImmutableEmoji.builder().name(emoji).build();
    }

    static Emoji custom(final Snowflake id, final String name) {
        return ImmutableEmoji.builder().id(id).name(name).build();
    }

    static Emoji custom(final Snowflake id, final String name, final boolean animated) {
        return ImmutableEmoji.builder().id(id).name(name).animated(animated).build();
    }

    /** Absent for unicode emojis. */
    Optional<Snowflake> id();

    /** The unicode emoji itself, or the custom emoji's name. Absent when the custom emoji was deleted. */
    Optional<String> name();

    @Value.Default
    default boolean animated() {
        return false;
    }
}
