// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import java.util.Optional;
import tools.jackson.databind.annotation.JsonDeserialize;

/** An emoji on a button, select option or reaction. */
@JsonDeserialize(using = EmojiDeserializer.class)
public sealed interface Emoji permits UnicodeEmoji, CustomEmoji {

    static UnicodeEmoji unicode(final String emoji) {
        return ImmutableUnicodeEmoji.of(emoji);
    }

    static CustomEmoji custom(final Snowflake id, final String name) {
        return custom(id, name, false);
    }

    static CustomEmoji custom(final Snowflake id, final String name, final boolean animated) {
        return ImmutableCustomEmoji.of(id, Optional.of(name), animated);
    }
}
