// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.DiscordEntityBehavior;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.user.UserBehavior;
import java.util.Optional;

public interface MemberBehavior extends DiscordEntityBehavior<Member> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static MemberBehavior of(
            @JacksonInject final Dukecord duke,
            @JacksonInject("guild_id") final Snowflake guildId,
            final Snowflake id) {
        return new MemberBehaviorImpl(duke, guildId, id);
    }

    Snowflake guildId();

    default GuildBehavior guild() {
        return GuildBehavior.of(this.duke(), this.guildId());
    }

    default UserBehavior user() {
        return UserBehavior.of(this.duke(), this.id());
    }

    default Optional<Member> get() {
        return this.duke().members().getByGuildAndId(this.guildId(), this.id());
    }

    @Override
    default Member fetch() {
        return this.duke().members().fetchByGuildAndId(this.guildId(), this.id());
    }

    @Override
    default Member fetch(final boolean force) {
        return this.duke().members().fetchByGuildAndId(this.guildId(), this.id(), force);
    }

    default Member edit(final MemberEditSpec spec) {
        return this.duke().members().rest().edit(this.guildId(), this.id(), spec);
    }

    default Member edit(final MemberEditSpec spec, final String reason) {
        return this.duke().members().rest().edit(this.guildId(), this.id(), spec, reason);
    }

    default void removeTimeout() {
        this.duke().members().rest().removeTimeout(this.guildId(), this.id());
    }

    default void removeTimeout(final String reason) {
        this.duke().members().rest().removeTimeout(this.guildId(), this.id(), reason);
    }

    default void addRole(final RoleBehavior role) {
        this.duke().members().rest().addRole(this.guildId(), this.id(), role.id());
    }

    default void addRole(final RoleBehavior role, final String reason) {
        this.duke().members().rest().addRole(this.guildId(), this.id(), role.id(), reason);
    }

    default void removeRole(final RoleBehavior role) {
        this.duke().members().rest().removeRole(this.guildId(), this.id(), role.id());
    }

    default void removeRole(final RoleBehavior role, final String reason) {
        this.duke().members().rest().removeRole(this.guildId(), this.id(), role.id(), reason);
    }

    default void kick() {
        this.duke().members().rest().kick(this.guildId(), this.id());
    }

    default void kick(final String reason) {
        this.duke().members().rest().kick(this.guildId(), this.id(), reason);
    }
}
