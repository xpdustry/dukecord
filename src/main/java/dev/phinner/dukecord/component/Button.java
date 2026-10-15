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
        return builder().style(Style.PRIMARY).customId(customId).label(label).build();
    }

    static Button secondary(final String customId, final String label) {
        return builder().style(Style.SECONDARY).customId(customId).label(label).build();
    }

    static Button success(final String customId, final String label) {
        return builder().style(Style.SUCCESS).customId(customId).label(label).build();
    }

    static Button danger(final String customId, final String label) {
        return builder().style(Style.DANGER).customId(customId).label(label).build();
    }

    static Button link(final String url, final String label) {
        return builder().style(Style.LINK).url(url).label(label).build();
    }

    @Override
    @Value.Derived
    default int type() {
        return 2;
    }

    Style style();

    Optional<String> label();

    Optional<Emoji> emoji();

    /** Absent on link and premium buttons. */
    Optional<String> customId();

    /** Only on link buttons. */
    Optional<String> url();

    /** Only on premium buttons. */
    Optional<Snowflake> skuId();

    @Value.Default
    default boolean disabled() {
        return false;
    }

    default Button withDisabled(final boolean disabled) {
        return ImmutableButton.copyOf(this).withDisabled(disabled);
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
        Builder from(Button instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder style(Style style);

        Builder label(String label);

        Builder label(Optional<String> label);

        Builder emoji(Emoji emoji);

        Builder emoji(Optional<? extends Emoji> emoji);

        Builder customId(String customId);

        Builder customId(Optional<String> customId);

        Builder url(String url);

        Builder url(Optional<String> url);

        Builder skuId(Snowflake skuId);

        Builder skuId(Optional<? extends Snowflake> skuId);

        Builder disabled(boolean disabled);

        Button build();
    }
}
