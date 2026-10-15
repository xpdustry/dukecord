// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import java.io.Serial;
import java.util.Optional;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

/** Discord sends both kinds of emoji as one object; custom emojis are the ones with an {@code id}. */
final class EmojiDeserializer extends StdDeserializer<Emoji> {

    @Serial
    private static final long serialVersionUID = 1L;

    EmojiDeserializer() {
        super(Emoji.class);
    }

    @Override
    public Emoji deserialize(final JsonParser p, final DeserializationContext ctxt) {
        final JsonNode node = ctxt.readTree(p);
        final var id = node.path("id");
        final var name = node.path("name");
        if (!id.isString()) {
            return Emoji.unicode(name.asString(""));
        }
        return ImmutableCustomEmoji.of(
                Snowflake.of(id.asString()),
                name.isString() ? Optional.of(name.asString()) : Optional.empty(),
                node.path("animated").asBoolean(false));
    }
}
