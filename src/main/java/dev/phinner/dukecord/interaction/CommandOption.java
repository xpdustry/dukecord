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
 * A command option, or a subcommand and group. The factories return the immutable type so that attributes chain, as in
 * {@code CommandOption.string("map", "The map").withRequired(true)}.
 */
@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableCommandOption.class)
public interface CommandOption {

    static ImmutableCommandOption string(final String name, final String description) {
        return of(Type.STRING, name, description);
    }

    static ImmutableCommandOption integer(final String name, final String description) {
        return of(Type.INTEGER, name, description);
    }

    static ImmutableCommandOption number(final String name, final String description) {
        return of(Type.NUMBER, name, description);
    }

    static ImmutableCommandOption bool(final String name, final String description) {
        return of(Type.BOOLEAN, name, description);
    }

    static ImmutableCommandOption user(final String name, final String description) {
        return of(Type.USER, name, description);
    }

    static ImmutableCommandOption channel(final String name, final String description) {
        return of(Type.CHANNEL, name, description);
    }

    static ImmutableCommandOption role(final String name, final String description) {
        return of(Type.ROLE, name, description);
    }

    static ImmutableCommandOption mentionable(final String name, final String description) {
        return of(Type.MENTIONABLE, name, description);
    }

    static ImmutableCommandOption attachment(final String name, final String description) {
        return of(Type.ATTACHMENT, name, description);
    }

    static ImmutableCommandOption subcommand(
            final String name, final String description, final CommandOption... options) {
        return of(Type.SUB_COMMAND, name, description).withOptions(options);
    }

    static ImmutableCommandOption group(
            final String name, final String description, final CommandOption... subcommands) {
        return of(Type.SUB_COMMAND_GROUP, name, description).withOptions(subcommands);
    }

    private static ImmutableCommandOption of(final Type type, final String name, final String description) {
        return ImmutableCommandOption.builder()
                .type(type)
                .name(name)
                .description(description)
                .build();
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
    }
}
