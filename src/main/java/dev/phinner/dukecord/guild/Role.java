// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import dev.phinner.dukecord.DiscordEntity;
import dev.phinner.dukecord.Snowflake;
import java.util.Optional;
import org.immutables.value.Value;
import tools.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonDeserialize(as = ImmutableRole.class)
public interface Role extends DiscordEntity, RoleBehavior {

    /** Discord's role objects leave the guild out; it comes from the surrounding payload. */
    @Override
    @JsonIgnore
    @JacksonInject(value = "guild_id", useInput = OptBoolean.FALSE)
    Snowflake guildId();

    String name();

    PermissionSet permissions();

    /** Higher is above. The {@code @everyone} role is at 0. */
    int position();

    @Value.Default
    default int color() {
        return 0;
    }

    /** Whether members are listed apart from online members. */
    @Value.Default
    default boolean hoist() {
        return false;
    }

    /** Whether an integration such as a bot manages the role. */
    @Value.Default
    default boolean managed() {
        return false;
    }

    @Value.Default
    default boolean mentionable() {
        return false;
    }

    Optional<String> icon();

    Optional<String> unicodeEmoji();

    @Value.Default
    default int flags() {
        return 0;
    }
}
