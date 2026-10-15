// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.Snowflake;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableAllowedMentions.class)
public interface AllowedMentions {

    static AllowedMentions none() {
        return ImmutableAllowedMentions.of();
    }

    static Builder builder() {
        return ImmutableAllowedMentions.builder();
    }

    Set<Type> parse();

    Set<Snowflake> users();

    Set<Snowflake> roles();

    @Value.Default
    default boolean repliedUser() {
        return false;
    }

    enum Type {
        @JsonProperty("users")
        USERS,
        @JsonProperty("roles")
        ROLES,
        @JsonProperty("everyone")
        EVERYONE
    }

    interface Builder {
        Builder from(final AllowedMentions instance);

        Builder addParse(final Type element);

        Builder addParse(final Type... elements);

        Builder parse(final Iterable<Type> elements);

        Builder addAllParse(final Iterable<Type> elements);

        Builder addUsers(final Snowflake element);

        Builder addUsers(final Snowflake... elements);

        Builder users(final Iterable<? extends Snowflake> elements);

        Builder addAllUsers(final Iterable<? extends Snowflake> elements);

        Builder addRoles(final Snowflake element);

        Builder addRoles(final Snowflake... elements);

        Builder roles(final Iterable<? extends Snowflake> elements);

        Builder addAllRoles(final Iterable<? extends Snowflake> elements);

        Builder repliedUser(final boolean repliedUser);

        AllowedMentions build();
    }
}
