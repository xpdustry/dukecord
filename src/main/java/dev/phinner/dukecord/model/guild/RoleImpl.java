// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.AbstractDiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.permission.PermissionSet;

final class RoleImpl extends AbstractDiscordEntity implements Role {

    private final DiscordEntityRef<Guild> guild;
    private final String name;
    private final int color;
    private final int position;
    private final PermissionSet permissions;
    private final boolean hoisted;
    private final boolean managed;
    private final boolean mentionable;

    RoleImpl(final Dukecord duke, final Snowflake guildId, final Json raw) {
        super(duke, raw);
        this.guild = Guilds.ref(duke, guildId);
        this.name = raw.string("name");
        this.color = raw.optionalObject("colors")
                .flatMap(colors -> colors.optionalInteger("primary_color"))
                .orElseGet(() -> raw.optionalInteger("color").orElse(0));
        this.position = raw.integer("position");
        this.permissions = PermissionSet.fromString(raw.string("permissions"));
        this.hoisted = raw.bool("hoist", false);
        this.managed = raw.bool("managed", false);
        this.mentionable = raw.bool("mentionable", false);
    }

    @Override
    public DiscordEntityRef<Guild> guild() {
        return this.guild;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int color() {
        return this.color;
    }

    @Override
    public int position() {
        return this.position;
    }

    @Override
    public PermissionSet permissions() {
        return this.permissions;
    }

    @Override
    public boolean isHoisted() {
        return this.hoisted;
    }

    @Override
    public boolean isManaged() {
        return this.managed;
    }

    @Override
    public boolean isMentionable() {
        return this.mentionable;
    }
}
