// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Vertical space between components, with an optional line. */
@Value.Immutable(singleton = true)
@JsonDeserialize(as = ImmutableSeparator.class)
public non-sealed interface Separator extends Component {

    static Builder builder() {
        return ImmutableSeparator.builder();
    }

    static Separator of() {
        return ImmutableSeparator.of();
    }

    @Override
    @Value.Derived
    default int type() {
        return 14;
    }

    /** Whether a line is drawn. Discord draws one by default. */
    Optional<Boolean> divider();

    Optional<Spacing> spacing();

    enum Spacing {
        @JsonEnumDefaultValue
        UNKNOWN(-1),
        SMALL(1),
        LARGE(2);

        private final int value;

        Spacing(final int value) {
            this.value = value;
        }

        @JsonValue
        public int value() {
            return this.value;
        }
    }

    interface Builder {
        Builder from(final Separator instance);

        Builder id(final int id);

        Builder divider(final boolean divider);

        Builder spacing(final Spacing spacing);

        Separator build();
    }
}
