// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * A text field in a modal, wrapped in a {@link Label}. The factories return the immutable type so that attributes
 * chain, as in {@code TextInput.singleLine("name").withRequired(true)}.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableTextInput.class)
public non-sealed interface TextInput extends Component {

    static Builder builder() {
        return ImmutableTextInput.builder();
    }

    static Builder singleLine(final String customId) {
        return builder().customId(customId).style(Style.SHORT);
    }

    static Builder paragraph(final String customId) {
        return builder().customId(customId).style(Style.PARAGRAPH);
    }

    @Override
    @Value.Derived
    default int type() {
        return 4;
    }

    String customId();

    /** Not sent back in a submitted modal. */
    @Value.Default
    default Style style() {
        return Style.SHORT;
    }

    /** The prefilled text, or in a submitted modal what the user typed. */
    Optional<String> value();

    Optional<String> placeholder();

    Optional<Boolean> required();

    Optional<Integer> minLength();

    Optional<Integer> maxLength();

    enum Style {
        @JsonEnumDefaultValue
        UNKNOWN(-1),
        SHORT(1),
        PARAGRAPH(2);

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
        Builder from(TextInput instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder customId(String customId);

        Builder style(Style style);

        Builder value(String value);

        Builder value(Optional<String> value);

        Builder placeholder(String placeholder);

        Builder placeholder(Optional<String> placeholder);

        Builder required(boolean required);

        Builder required(Optional<Boolean> required);

        Builder minLength(int minLength);

        Builder minLength(Optional<Integer> minLength);

        Builder maxLength(int maxLength);

        Builder maxLength(Optional<Integer> maxLength);

        TextInput build();
    }
}
