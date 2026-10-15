// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.guild;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import dev.phinner.dukecord.DiscordEntityBehavior;
import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.Snowflake;
import dev.phinner.dukecord.channel.CategoryChannel;
import dev.phinner.dukecord.channel.ChannelCreateSpec;
import dev.phinner.dukecord.channel.GuildTextChannel;
import java.util.Optional;

public interface GuildBehavior extends DiscordEntityBehavior<Guild> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static GuildBehavior of(@JacksonInject final Dukecord duke, final Snowflake id) {
        return new GuildBehaviorImpl(duke, id);
    }

    default Optional<Guild> get() {
        return this.duke().guilds().getById(this.id());
    }

    @Override
    default Guild fetch() {
        return this.duke().guilds().fetchById(this.id());
    }

    @Override
    default Guild fetch(final boolean force) {
        return this.duke().guilds().fetchById(this.id(), force);
    }

    default MemberBehavior member(final Snowflake userId) {
        return MemberBehavior.of(this.duke(), this.id(), userId);
    }

    default RoleBehavior role(final Snowflake roleId) {
        return RoleBehavior.of(this.duke(), this.id(), roleId);
    }

    /** The role every member has, which shares the guild's ID. */
    default RoleBehavior everyoneRole() {
        return this.role(this.id());
    }

    default Role createRole(final RoleCreateSpec spec) {
        return this.duke().roles().rest().create(this.id(), spec);
    }

    default Role createRole(final RoleCreateSpec spec, final String reason) {
        return this.duke().roles().rest().create(this.id(), spec, reason);
    }

    default GuildTextChannel createTextChannel(final String name) {
        return this.createTextChannel(ChannelCreateSpec.of(name));
    }

    default GuildTextChannel createTextChannel(final ChannelCreateSpec spec) {
        return this.duke().channels().rest().createTextChannel(this.id(), spec);
    }

    default GuildTextChannel createTextChannel(final ChannelCreateSpec spec, final String reason) {
        return this.duke().channels().rest().createTextChannel(this.id(), spec, reason);
    }

    default CategoryChannel createCategory(final String name) {
        return this.createCategory(ChannelCreateSpec.of(name));
    }

    default CategoryChannel createCategory(final ChannelCreateSpec spec) {
        return this.duke().channels().rest().createCategory(this.id(), spec);
    }

    default CategoryChannel createCategory(final ChannelCreateSpec spec, final String reason) {
        return this.duke().channels().rest().createCategory(this.id(), spec, reason);
    }
}
