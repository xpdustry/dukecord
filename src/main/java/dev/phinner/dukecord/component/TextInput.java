// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * A text field in a modal, wrapped in a {@link Label}, as in
 * {@code Label.of("Name", TextInput.singleLine("name", true))}.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableTextInput.class)
public non-sealed interface TextInput extends Component {

    static Builder builder() {
        return ImmutableTextInput.builder();
    }

    static TextInput singleLine(final String customId) {
        return ImmutableTextInput.of(customId, Style.SHORT, Optional.empty());
    }

    static TextInput singleLine(final String customId, final boolean required) {
        return ImmutableTextInput.of(customId, Style.SHORT, Optional.of(required));
    }

    static TextInput paragraph(final String customId) {
        return ImmutableTextInput.of(customId, Style.PARAGRAPH, Optional.empty());
    }

    static TextInput paragraph(final String customId, final boolean required) {
        return ImmutableTextInput.of(customId, Style.PARAGRAPH, Optional.of(required));
    }

    @Override
    @Value.Derived
    default int type() {
        return 4;
    }

    @Value.Parameter
    String customId();

    /** Not sent back in a submitted modal. */
    @Value.Default
    @Value.Parameter
    default Style style() {
        return Style.SHORT;
    }

    /** The prefilled text, or in a submitted modal what the user typed. */
    Optional<String> value();

    Optional<String> placeholder();

    @Value.Parameter
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
        Builder from(final TextInput instance);

        Builder id(final int id);

        Builder customId(final String customId);

        Builder style(final Style style);

        Builder value(final String value);

        Builder placeholder(final String placeholder);

        Builder required(final boolean required);

        Builder minLength(final int minLength);

        Builder maxLength(final int maxLength);

        TextInput build();
    }
}
