// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.guild.Role;
import dev.phinner.dukecord.message.Attachment;
import dev.phinner.dukecord.user.User;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** The invoked command and its options. Option lookups search the options of the invoked subcommand. */
@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableCommandInteractionData.class)
public interface CommandInteractionData {

    /** The command's ID. */
    Snowflake id();

    String name();

    ApplicationCommand.Type type();

    List<Option> options();

    @Value.Default
    default Resolved resolved() {
        return Resolved.empty();
    }

    /** Set when the command is registered in a guild. */
    Optional<Snowflake> guildId();

    /** The user or message of a user or message command. */
    Optional<Snowflake> targetId();

    /** The command name followed by the subcommand group and subcommand, such as {@code "map search"}. */
    default String path() {
        final var path = new StringBuilder(this.name());
        var options = this.options();
        while (options.size() == 1 && options.getFirst().subcommand()) {
            path.append(' ').append(options.getFirst().name());
            options = options.getFirst().options();
        }
        return path.toString();
    }

    /** The options of the invoked subcommand, or of the command if it has none. */
    default List<Option> leafOptions() {
        var options = this.options();
        while (options.size() == 1 && options.getFirst().subcommand()) {
            options = options.getFirst().options();
        }
        return options;
    }

    default Optional<Option> option(final String name) {
        return this.leafOptions().stream()
                .filter(option -> option.name().equals(name))
                .findFirst();
    }

    default Optional<String> stringOption(final String name) {
        return this.option(name).flatMap(Option::value).map(String::valueOf);
    }

    default Optional<Long> longOption(final String name) {
        return this.option(name).flatMap(Option::value).map(value -> ((Number) value).longValue());
    }

    default Optional<Double> doubleOption(final String name) {
        return this.option(name).flatMap(Option::value).map(value -> ((Number) value).doubleValue());
    }

    default Optional<Boolean> booleanOption(final String name) {
        return this.option(name).flatMap(Option::value).map(Boolean.class::cast);
    }

    /** The ID in a user, role, channel, mentionable or attachment option. */
    default Optional<Snowflake> snowflakeOption(final String name) {
        return this.stringOption(name).map(Snowflake::of);
    }

    default Optional<User> userOption(final String name) {
        return this.snowflakeOption(name).map(id -> this.resolved().users().get(id));
    }

    default Optional<Role> roleOption(final String name) {
        return this.snowflakeOption(name).map(id -> this.resolved().roles().get(id));
    }

    default Optional<Attachment> attachmentOption(final String name) {
        return this.snowflakeOption(name)
                .map(id -> this.resolved().attachments().get(id));
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableCommandInteractionData.Option.class)
    interface Option {

        String name();

        CommandOption.Type type();

        /** A string, number or boolean. IDs come as strings. Absent on subcommands and groups. */
        Optional<Object> value();

        /** The options of a subcommand or group. */
        List<Option> options();

        /** Whether the user is typing this option, only in autocomplete. */
        @Value.Default
        default boolean focused() {
            return false;
        }

        default boolean subcommand() {
            return this.type() == CommandOption.Type.SUB_COMMAND || this.type() == CommandOption.Type.SUB_COMMAND_GROUP;
        }
    }
}
