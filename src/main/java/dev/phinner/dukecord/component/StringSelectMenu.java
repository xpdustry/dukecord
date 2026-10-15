// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.Emoji;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableStringSelectMenu.class)
public non-sealed interface StringSelectMenu extends Component {

    static ImmutableStringSelectMenu.Builder builder() {
        return ImmutableStringSelectMenu.builder();
    }

    @Override
    @Value.Derived
    default int type() {
        return 3;
    }

    String customId();

    List<Option> options();

    Optional<String> placeholder();

    Optional<Integer> minValues();

    Optional<Integer> maxValues();

    /** Only inside a modal. */
    Optional<Boolean> required();

    @Value.Default
    default boolean disabled() {
        return false;
    }

    /** What the user picked, only in a submitted modal. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> values();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableStringSelectMenu.Option.class)
    interface Option {

        static ImmutableStringSelectMenu.Option.Builder builder() {
            return ImmutableStringSelectMenu.Option.builder();
        }

        static Option of(final String label, final String value) {
            return builder().label(label).value(value).build();
        }

        String label();

        String value();

        Optional<String> description();

        Optional<Emoji> emoji();

        /** Whether the option starts selected. */
        @JsonProperty("default")
        @Value.Default
        default boolean selected() {
            return false;
        }
    }
}
