// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.guild.Guilds;
import dev.phinner.dukecord.model.permission.PermissionOverwrite;
import java.util.List;
import java.util.Optional;

abstract class AbstractGuildChannel extends AbstractChannel {

    private final Snowflake guildId;
    private final DiscordEntityRef<Guild> guild;
    private final String name;
    private final int position;
    private final Optional<DiscordEntityRef<CategoryChannel>> category;
    private final List<PermissionOverwrite> overwrites;
    private final int flags;

    AbstractGuildChannel(final Dukecord duke, final Json raw) {
        super(duke, raw);
        this.guildId = raw.snowflake("guild_id");
        this.guild = Guilds.ref(duke, this.guildId);
        this.name = raw.optionalString("name").orElse("");
        this.position = raw.optionalInteger("position").orElse(0);
        this.category = this.type().isThread()
                ? Optional.empty()
                : raw.optionalSnowflake("parent_id")
                        .map(parentId -> Channels.ref(duke, CategoryChannel.class, parentId));
        this.overwrites = raw.objects("permission_overwrites").stream()
                .map(PermissionOverwrite::fromJson)
                .toList();
        this.flags = raw.optionalInteger("flags").orElse(0);
    }

    final Snowflake guildId() {
        return this.guildId;
    }

    public final DiscordEntityRef<Guild> guild() {
        return this.guild;
    }

    public final String name() {
        return this.name;
    }

    public final int position() {
        return this.position;
    }

    public final Optional<DiscordEntityRef<CategoryChannel>> category() {
        return this.category;
    }

    public final List<PermissionOverwrite> overwrites() {
        return this.overwrites;
    }

    public final int flags() {
        return this.flags;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName().replace("Impl", "") + "[" + this.id() + " #" + this.name + "]";
    }
}
