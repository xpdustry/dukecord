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

    static StringSelectMenu of(final String customId, final Option... options) {
        return ImmutableStringSelectMenu.of(customId, List.of(options));
    }

    @Override
    @Value.Derived
    default int type() {
        return 3;
    }

    @Value.Parameter
    String customId();

    @Value.Parameter
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
            return ImmutableStringSelectMenu.Option.of(label, value);
        }

        @Value.Parameter
        String label();

        @Value.Parameter
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
            Builder from(final Option instance);

            Builder label(final String label);

            Builder value(final String value);

            Builder description(final String description);

            Builder emoji(final Emoji emoji);

            Builder selected(final boolean selected);

            Option build();
        }
    }

    interface Builder {
        Builder from(final StringSelectMenu instance);

        Builder id(final int id);

        Builder customId(final String customId);

        Builder addOptions(final Option element);

        Builder addOptions(final Option... elements);

        Builder options(final Iterable<? extends Option> elements);

        Builder addAllOptions(final Iterable<? extends Option> elements);

        Builder placeholder(final String placeholder);

        Builder minValues(final int minValues);

        Builder maxValues(final int maxValues);

        Builder required(final boolean required);

        Builder disabled(final boolean disabled);

        Builder addValues(final String element);

        Builder addValues(final String... elements);

        Builder values(final Iterable<String> elements);

        Builder addAllValues(final Iterable<String> elements);

        StringSelectMenu build();
    }
}
