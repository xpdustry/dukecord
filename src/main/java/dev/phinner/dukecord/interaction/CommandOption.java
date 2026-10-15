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
 * A command option, or a subcommand and group. The factories cover the common cases, such as
 * {@code CommandOption.string("map", "The map", true)}, and {@link #builder()} the rest, such as autocomplete or
 * value ranges.
 */
@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableCommandOption.class)
public interface CommandOption {

    static Builder builder() {
        return ImmutableCommandOption.builder();
    }

    static CommandOption string(final String name, final String description) {
        return of(Type.STRING, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption string(final String name, final String description, final boolean required) {
        return of(Type.STRING, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption string(
            final String name, final String description, final boolean required, final Choice... choices) {
        return of(Type.STRING, name, description, Optional.of(required), List.of(choices), List.of());
    }

    static CommandOption integer(final String name, final String description) {
        return of(Type.INTEGER, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption integer(final String name, final String description, final boolean required) {
        return of(Type.INTEGER, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption integer(
            final String name, final String description, final boolean required, final Choice... choices) {
        return of(Type.INTEGER, name, description, Optional.of(required), List.of(choices), List.of());
    }

    static CommandOption number(final String name, final String description) {
        return of(Type.NUMBER, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption number(final String name, final String description, final boolean required) {
        return of(Type.NUMBER, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption number(
            final String name, final String description, final boolean required, final Choice... choices) {
        return of(Type.NUMBER, name, description, Optional.of(required), List.of(choices), List.of());
    }

    static CommandOption bool(final String name, final String description) {
        return of(Type.BOOLEAN, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption bool(final String name, final String description, final boolean required) {
        return of(Type.BOOLEAN, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption user(final String name, final String description) {
        return of(Type.USER, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption user(final String name, final String description, final boolean required) {
        return of(Type.USER, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption channel(final String name, final String description) {
        return of(Type.CHANNEL, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption channel(final String name, final String description, final boolean required) {
        return of(Type.CHANNEL, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption role(final String name, final String description) {
        return of(Type.ROLE, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption role(final String name, final String description, final boolean required) {
        return of(Type.ROLE, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption mentionable(final String name, final String description) {
        return of(Type.MENTIONABLE, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption mentionable(final String name, final String description, final boolean required) {
        return of(Type.MENTIONABLE, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption attachment(final String name, final String description) {
        return of(Type.ATTACHMENT, name, description, Optional.empty(), List.of(), List.of());
    }

    static CommandOption attachment(final String name, final String description, final boolean required) {
        return of(Type.ATTACHMENT, name, description, Optional.of(required), List.of(), List.of());
    }

    static CommandOption subcommand(final String name, final String description, final CommandOption... options) {
        return of(Type.SUB_COMMAND, name, description, Optional.empty(), List.of(), List.of(options));
    }

    static CommandOption group(final String name, final String description, final CommandOption... subcommands) {
        return of(Type.SUB_COMMAND_GROUP, name, description, Optional.empty(), List.of(), List.of(subcommands));
    }

    private static CommandOption of(
            final Type type,
            final String name,
            final String description,
            final Optional<Boolean> required,
            final List<Choice> choices,
            final List<CommandOption> options) {
        return ImmutableCommandOption.of(type, name, description, required, choices, options);
    }

    @Value.Parameter
    Type type();

    @Value.Parameter
    String name();

    @Value.Parameter
    String description();

    @Value.Parameter
    Optional<Boolean> required();

    /** The only values allowed, up to 25. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Value.Parameter
    List<Choice> choices();

    /** Whether choices come from {@link AutocompleteInteractionEvent}s. Excludes {@link #choices()}. */
    Optional<Boolean> autocomplete();

    /** The options of a subcommand, or the subcommands of a group. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Value.Parameter
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
            return ImmutableCommandOption.Choice.of(name, value);
        }

        static Choice of(final String name, final long value) {
            return ImmutableCommandOption.Choice.of(name, value);
        }

        static Choice of(final String name, final double value) {
            return ImmutableCommandOption.Choice.of(name, value);
        }

        @Value.Parameter
        String name();

        /** A string, integer or double, matching the option type. */
        @Value.Parameter
        Object value();

        interface Builder {
            Builder from(final Choice instance);

            Builder name(final String name);

            Builder value(final Object value);

            Choice build();
        }
    }

    interface Builder {
        Builder from(final CommandOption instance);

        Builder type(final Type type);

        Builder name(final String name);

        Builder description(final String description);

        Builder required(final boolean required);

        Builder addChoices(final Choice element);

        Builder addChoices(final Choice... elements);

        Builder choices(final Iterable<? extends Choice> elements);

        Builder addAllChoices(final Iterable<? extends Choice> elements);

        Builder autocomplete(final boolean autocomplete);

        Builder addOptions(final CommandOption element);

        Builder addOptions(final CommandOption... elements);

        Builder options(final Iterable<? extends CommandOption> elements);

        Builder addAllOptions(final Iterable<? extends CommandOption> elements);

        Builder addChannelTypes(final int element);

        Builder addChannelTypes(final int... elements);

        Builder channelTypes(final Iterable<Integer> elements);

        Builder addAllChannelTypes(final Iterable<Integer> elements);

        Builder minValue(final Number minValue);

        Builder maxValue(final Number maxValue);

        Builder minLength(final int minLength);

        Builder maxLength(final int maxLength);

        CommandOption build();
    }
}
