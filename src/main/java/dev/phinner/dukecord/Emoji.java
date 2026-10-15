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

    static Builder builder() {
        return ImmutableEmoji.builder();
    }

    static Emoji unicode(final String emoji) {
        return ImmutableEmoji.of(Optional.empty(), Optional.of(emoji), false);
    }

    static Emoji custom(final Snowflake id, final String name) {
        return custom(id, name, false);
    }

    static Emoji custom(final Snowflake id, final String name, final boolean animated) {
        return ImmutableEmoji.of(Optional.of(id), Optional.of(name), animated);
    }

    /** Absent for unicode emojis. */
    @Value.Parameter
    Optional<Snowflake> id();

    /** The unicode emoji itself, or the custom emoji's name. Absent when the custom emoji was deleted. */
    @Value.Parameter
    Optional<String> name();

    @Value.Default
    @Value.Parameter
    default boolean animated() {
        return false;
    }

    interface Builder {
        Builder from(final Emoji instance);

        Builder id(final Snowflake id);

        Builder name(final String name);

        Builder animated(final boolean animated);

        Emoji build();
    }
}
