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

    static ImmutableTextInput.Builder builder() {
        return ImmutableTextInput.builder();
    }

    static ImmutableTextInput singleLine(final String customId) {
        return builder().customId(customId).style(Style.SHORT).build();
    }

    static ImmutableTextInput paragraph(final String customId) {
        return builder().customId(customId).style(Style.PARAGRAPH).build();
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
}
