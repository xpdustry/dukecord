// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.phinner.dukecord.Emoji;
import dev.phinner.dukecord.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableButton.class)
public non-sealed interface Button extends Component {

    static Builder builder() {
        return ImmutableButton.builder();
    }

    static Button primary(final String customId, final String label) {
        return ImmutableButton.of(Style.PRIMARY, Optional.of(label), Optional.of(customId), Optional.empty());
    }

    static Button secondary(final String customId, final String label) {
        return ImmutableButton.of(Style.SECONDARY, Optional.of(label), Optional.of(customId), Optional.empty());
    }

    static Button success(final String customId, final String label) {
        return ImmutableButton.of(Style.SUCCESS, Optional.of(label), Optional.of(customId), Optional.empty());
    }

    static Button danger(final String customId, final String label) {
        return ImmutableButton.of(Style.DANGER, Optional.of(label), Optional.of(customId), Optional.empty());
    }

    static Button link(final String url, final String label) {
        return ImmutableButton.of(Style.LINK, Optional.of(label), Optional.empty(), Optional.of(url));
    }

    @Override
    @Value.Derived
    default int type() {
        return 2;
    }

    @Value.Parameter
    Style style();

    @Value.Parameter
    Optional<String> label();

    Optional<Emoji> emoji();

    /** Absent on link and premium buttons. */
    @Value.Parameter
    Optional<String> customId();

    /** Only on link buttons. */
    @Value.Parameter
    Optional<String> url();

    /** Only on premium buttons. */
    Optional<Snowflake> skuId();

    @Value.Default
    default boolean disabled() {
        return false;
    }

    @Override
    default Button withDisabled(final boolean disabled) {
        return Button.builder().from(this).disabled(disabled).build();
    }

    default Button withEmoji(final Emoji emoji) {
        return Button.builder().from(this).emoji(emoji).build();
    }

    enum Style {
        @JsonEnumDefaultValue
        UNKNOWN(-1),
        PRIMARY(1),
        SECONDARY(2),
        SUCCESS(3),
        DANGER(4),
        LINK(5),
        PREMIUM(6);

        private final int value;

        Style(final int value) {
            this.value = value;
        }

        @JsonValue
        public int value() {
            return this.value;
        }
    }

    interface Builder {
        Builder from(final Button instance);

        Builder id(final int id);

        Builder style(final Style style);

        Builder label(final String label);

        Builder emoji(final Emoji emoji);

        Builder customId(final String customId);

        Builder url(final String url);

        Builder skuId(final Snowflake skuId);

        Builder disabled(final boolean disabled);

        Button build();
    }
}
