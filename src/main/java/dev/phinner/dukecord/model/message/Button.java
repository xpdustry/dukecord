// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.message;

import dev.phinner.dukecord.model.Emoji;
import dev.phinner.dukecord.model.Json;
import java.util.Optional;

public record Button(
        Style style,
        Optional<String> customId,
        Optional<String> label,
        Optional<Emoji> emoji,
        Optional<String> url,
        boolean disabled)
        implements Component {

    public enum Style {
        PRIMARY(1),
        SECONDARY(2),
        SUCCESS(3),
        DANGER(4),
        LINK(5),
        PREMIUM(6);

        private final int code;

        Style(final int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }

        public static Style fromCode(final int code) {
            for (final var style : values()) {
                if (style.code == code) {
                    return style;
                }
            }
            return SECONDARY;
        }
    }

    public static Button primary(final String customId, final String label) {
        return of(Style.PRIMARY, customId, label);
    }

    public static Button secondary(final String customId, final String label) {
        return of(Style.SECONDARY, customId, label);
    }

    public static Button success(final String customId, final String label) {
        return of(Style.SUCCESS, customId, label);
    }

    public static Button danger(final String customId, final String label) {
        return of(Style.DANGER, customId, label);
    }

    public static Button link(final String url, final String label) {
        return new Button(Style.LINK, Optional.empty(), Optional.of(label), Optional.empty(), Optional.of(url), false);
    }

    private static Button of(final Style style, final String customId, final String label) {
        return new Button(style, Optional.of(customId), Optional.of(label), Optional.empty(), Optional.empty(), false);
    }

    public Button withDisabled(final boolean disabled) {
        return new Button(this.style, this.customId, this.label, this.emoji, this.url, disabled);
    }

    public Button withEmoji(final Emoji emoji) {
        return new Button(this.style, this.customId, this.label, Optional.of(emoji), this.url, this.disabled);
    }

    @Override
    public int type() {
        return TYPE_BUTTON;
    }

    @Override
    public Json toJson() {
        var json = Json.empty()
                .with("type", TYPE_BUTTON)
                .with("style", this.style.code())
                .with("disabled", this.disabled);
        if (this.customId.isPresent()) {
            json = json.with("custom_id", this.customId.get());
        }
        if (this.label.isPresent()) {
            json = json.with("label", this.label.get());
        }
        if (this.emoji.isPresent()) {
            json = json.with("emoji", this.emoji.get().toJson());
        }
        if (this.url.isPresent()) {
            json = json.with("url", this.url.get());
        }
        return json;
    }

    @Override
    public Button asDisabled() {
        return this.withDisabled(true);
    }
}
