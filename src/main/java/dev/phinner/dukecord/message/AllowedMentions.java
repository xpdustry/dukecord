// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.phinner.dukecord.Snowflake;
import java.util.Set;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableAllowedMentions.class)
public interface AllowedMentions {

    static AllowedMentions none() {
        return builder().build();
    }

    static ImmutableAllowedMentions.Builder builder() {
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
}
