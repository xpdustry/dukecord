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

    static Builder builder() {
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

        static Builder builder() {
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

        interface Builder {
            Builder from(Option instance);

            Builder label(String label);

            Builder value(String value);

            Builder description(String description);

            Builder description(Optional<String> description);

            Builder emoji(Emoji emoji);

            Builder emoji(Optional<? extends Emoji> emoji);

            Builder selected(boolean selected);

            Option build();
        }
    }

    interface Builder {
        Builder from(StringSelectMenu instance);

        Builder id(int id);

        Builder id(Optional<Integer> id);

        Builder customId(String customId);

        Builder addOptions(Option element);

        Builder addOptions(Option... elements);

        Builder options(Iterable<? extends Option> elements);

        Builder addAllOptions(Iterable<? extends Option> elements);

        Builder placeholder(String placeholder);

        Builder placeholder(Optional<String> placeholder);

        Builder minValues(int minValues);

        Builder minValues(Optional<Integer> minValues);

        Builder maxValues(int maxValues);

        Builder maxValues(Optional<Integer> maxValues);

        Builder required(boolean required);

        Builder required(Optional<Boolean> required);

        Builder disabled(boolean disabled);

        Builder addValues(String element);

        Builder addValues(String... elements);

        Builder values(Iterable<String> elements);

        Builder addAllValues(Iterable<String> elements);

        StringSelectMenu build();
    }
}
