// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A standard emoji, such as {@code ✅}. */
@Value.Immutable
@JsonDeserialize(as = ImmutableUnicodeEmoji.class)
public non-sealed interface UnicodeEmoji extends Emoji {

    static Builder builder() {
        return ImmutableUnicodeEmoji.builder();
    }

    /** The emoji itself. */
    @Value.Parameter
    String name();

    interface Builder {
        Builder from(final UnicodeEmoji instance);

        Builder name(final String name);

        UnicodeEmoji build();
    }
}
