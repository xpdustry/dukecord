// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.interaction;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import dev.phinner.dukecord.DiscordEntity;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.guild.PermissionSet;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

/** A registered command. */
@Value.Immutable
@JsonDeserialize(as = ImmutableApplicationCommand.class)
public interface ApplicationCommand extends DiscordEntity {

    Snowflake applicationId();

    /** Absent for global commands. */
    Optional<Snowflake> guildId();

    @Value.Default
    default Type type() {
        return Type.CHAT_INPUT;
    }

    String name();

    /** Empty for user and message commands. */
    @Value.Default
    default String description() {
        return "";
    }

    List<CommandOption> options();

    Optional<PermissionSet> defaultMemberPermissions();

    @Value.Default
    default boolean nsfw() {
        return false;
    }

    /** Changes on every update of the command. */
    Snowflake version();

    enum Type {
        @JsonEnumDefaultValue
        UNKNOWN(-1),
        /** A slash command. */
        CHAT_INPUT(1),
        /** A command in a user's context menu. */
        USER(2),
        /** A command in a message's context menu. */
        MESSAGE(3);

        private final int value;

        Type(final int value) {
            this.value = value;
        }

        @JsonValue
        public int value() {
            return this.value;
        }
    }
}
