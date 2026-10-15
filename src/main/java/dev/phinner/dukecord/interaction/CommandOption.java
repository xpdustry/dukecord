// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * A command option, or a subcommand and group. The factories return a builder so that attributes chain, as in
 * {@code CommandOption.string("map", "The map").required(true).build()}.
 */
@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableCommandOption.class)
public interface CommandOption {

    static Builder builder() {
        return ImmutableCommandOption.builder();
    }

    static Builder string(final String name, final String description) {
        return of(Type.STRING, name, description);
    }

    static Builder integer(final String name, final String description) {
        return of(Type.INTEGER, name, description);
    }

    static Builder number(final String name, final String description) {
        return of(Type.NUMBER, name, description);
    }

    static Builder bool(final String name, final String description) {
        return of(Type.BOOLEAN, name, description);
    }

    static Builder user(final String name, final String description) {
        return of(Type.USER, name, description);
    }

    static Builder channel(final String name, final String description) {
        return of(Type.CHANNEL, name, description);
    }

    static Builder role(final String name, final String description) {
        return of(Type.ROLE, name, description);
    }

    static Builder mentionable(final String name, final String description) {
        return of(Type.MENTIONABLE, name, description);
    }

    static Builder attachment(final String name, final String description) {
        return of(Type.ATTACHMENT, name, description);
    }

    static Builder subcommand(final String name, final String description, final CommandOption... options) {
        return of(Type.SUB_COMMAND, name, description).addOptions(options);
    }

    static Builder group(final String name, final String description, final CommandOption... subcommands) {
        return of(Type.SUB_COMMAND_GROUP, name, description).addOptions(subcommands);
    }

    private static Builder of(final Type type, final String name, final String description) {
        return ImmutableCommandOption.builder().type(type).name(name).description(description);
    }

    Type type();

    String name();

    String description();

    Optional<Boolean> required();

    /** The only values allowed, up to 25. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Choice> choices();

    /** Whether choices come from {@link AutocompleteInteractionEvent}s. Excludes {@link #choices()}. */
    Optional<Boolean> autocomplete();

    /** The options of a subcommand, or the subcommands of a group. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<CommandOption> options();

    /** Restricts a channel option to these channel types. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<Integer> channelTypes();

    Optional<Number> minValue();

    Optional<Number> maxValue();

    Optional<Integer> minLength();

    Optional<Integer> maxLength();

    enum Type {
        @JsonEnumDefaultValue
        UNKNOWN(-1),
        SUB_COMMAND(1),
        SUB_COMMAND_GROUP(2),
        STRING(3),
        INTEGER(4),
        BOOLEAN(5),
        USER(6),
        CHANNEL(7),
        ROLE(8),
        MENTIONABLE(9),
        NUMBER(10),
        ATTACHMENT(11);

        private final int value;

        Type(final int value) {
            this.value = value;
        }

        @JsonValue
        public int value() {
            return this.value;
        }
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableCommandOption.Choice.class)
    interface Choice {

        static Builder builder() {
            return ImmutableCommandOption.Choice.builder();
        }

        static Choice of(final String name, final String value) {
            return ImmutableCommandOption.Choice.builder()
                    .name(name)
                    .value(value)
                    .build();
        }

        static Choice of(final String name, final long value) {
            return ImmutableCommandOption.Choice.builder()
                    .name(name)
                    .value(value)
                    .build();
        }

        static Choice of(final String name, final double value) {
            return ImmutableCommandOption.Choice.builder()
                    .name(name)
                    .value(value)
                    .build();
        }

        String name();

        /** A string, integer or double, matching the option type. */
        Object value();

        interface Builder {
            Builder from(Choice instance);

            Builder name(String name);

            Builder value(Object value);

            Choice build();
        }
    }

    interface Builder {
        Builder from(CommandOption instance);

        Builder type(Type type);

        Builder name(String name);

        Builder description(String description);

        Builder required(boolean required);

        Builder required(Optional<Boolean> required);

        Builder addChoices(Choice element);

        Builder addChoices(Choice... elements);

        Builder choices(Iterable<? extends Choice> elements);

        Builder addAllChoices(Iterable<? extends Choice> elements);

        Builder autocomplete(boolean autocomplete);

        Builder autocomplete(Optional<Boolean> autocomplete);

        Builder addOptions(CommandOption element);

        Builder addOptions(CommandOption... elements);

        Builder options(Iterable<? extends CommandOption> elements);

        Builder addAllOptions(Iterable<? extends CommandOption> elements);

        Builder addChannelTypes(int element);

        Builder addChannelTypes(int... elements);

        Builder channelTypes(Iterable<Integer> elements);

        Builder addAllChannelTypes(Iterable<Integer> elements);

        Builder minValue(Number minValue);

        Builder minValue(Optional<? extends Number> minValue);

        Builder maxValue(Number maxValue);

        Builder maxValue(Optional<? extends Number> maxValue);

        Builder minLength(int minLength);

        Builder minLength(Optional<Integer> minLength);

        Builder maxLength(int maxLength);

        Builder maxLength(Optional<Integer> maxLength);

        CommandOption build();
    }
}
