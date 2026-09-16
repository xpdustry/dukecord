// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** A unicode emoji ({@code id} empty) or a custom guild emoji. */
public record Emoji(Optional<Snowflake> id, String name, boolean animated) {

    public static Emoji unicode(final String name) {
        return new Emoji(Optional.empty(), name, false);
    }

    public static Emoji custom(final Snowflake id, final String name, final boolean animated) {
        return new Emoji(Optional.of(id), name, animated);
    }

    public static Emoji fromJson(final Json json) {
        return new Emoji(
                json.optionalSnowflake("id"), json.optionalString("name").orElse(""), json.bool("animated", false));
    }

    public Json toJson() {
        var json = Json.empty().with("name", this.name);
        if (this.id.isPresent()) {
            json = json.with("id", this.id.get().asString()).with("animated", this.animated);
        }
        return json;
    }

    public boolean isCustom() {
        return this.id.isPresent();
    }

    /** The form used in message content: the character itself, or {@code <:name:id>}. */
    public String mention() {
        return this.id
                .map(id -> "<" + (this.animated ? "a" : "") + ":" + this.name + ":" + id.asString() + ">")
                .orElse(this.name);
    }

    /** The URL-encoded form used in reaction routes: {@code name:id} for custom emoji, the character otherwise. */
    public String asReactionPath() {
        final var value = this.id.map(id -> this.name + ":" + id.asString()).orElse(this.name);
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
