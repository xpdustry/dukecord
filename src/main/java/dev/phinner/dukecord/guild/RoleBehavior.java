// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.DiscordEntityBehavior;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import java.util.Optional;

public interface RoleBehavior extends DiscordEntityBehavior<Role> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static RoleBehavior of(
            @JacksonInject final Dukecord duke,
            @JacksonInject("guild_id") final Snowflake guildId,
            final Snowflake id) {
        return new RoleBehaviorImpl(duke, guildId, id);
    }

    Snowflake guildId();

    default GuildBehavior guild() {
        return GuildBehavior.of(this.duke(), this.guildId());
    }

    /** Whether this is the {@code @everyone} role, which shares the guild's ID. */
    default boolean everyone() {
        return this.id().equals(this.guildId());
    }

    default Optional<Role> get() {
        return this.duke().roles().getByGuildAndId(this.guildId(), this.id());
    }

    @Override
    default Role fetch() {
        return this.duke().roles().fetchByGuildAndId(this.guildId(), this.id());
    }

    @Override
    default Role fetch(final boolean force) {
        return this.duke().roles().fetchByGuildAndId(this.guildId(), this.id(), force);
    }

    default Role edit(final RoleEditSpec spec) {
        return this.duke().roles().rest().edit(this.guildId(), this.id(), spec);
    }

    default Role edit(final RoleEditSpec spec, final String reason) {
        return this.duke().roles().rest().edit(this.guildId(), this.id(), spec, reason);
    }

    default void delete() {
        this.duke().roles().rest().delete(this.guildId(), this.id());
    }

    default void delete(final String reason) {
        this.duke().roles().rest().delete(this.guildId(), this.id(), reason);
    }
}
