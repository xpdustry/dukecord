// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.Dukecord;
import dev.phinner.dukecord.model.AbstractDiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.DiscordEntityRefList;
import dev.phinner.dukecord.model.Json;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.permission.PermissionSet;
import dev.phinner.dukecord.model.user.User;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

final class MemberImpl extends AbstractDiscordEntity implements Member {

    private final DiscordEntityRef<Guild> guild;
    private final User user;
    private final Optional<String> nickname;
    private final Optional<String> avatarHash;
    private final DiscordEntityRefList<Role> roles;
    private final Optional<Instant> joinedAt;
    private final Optional<Instant> boostingSince;
    private final Optional<Instant> timeoutUntil;
    private final boolean pending;
    private final EnumSet<MemberFlag> flags;
    private final Optional<PermissionSet> permissions;

    MemberImpl(final Dukecord duke, final Snowflake guildId, final Json raw, final User user) {
        super(duke, raw, user.id());
        this.guild = Guilds.ref(duke, guildId);
        this.user = user;
        this.nickname = raw.optionalString("nick");
        this.avatarHash = raw.optionalString("avatar");
        this.roles = Roles.refs(duke, guildId, raw.snowflakes("roles"));
        this.joinedAt = raw.optionalInstant("joined_at");
        this.boostingSince = raw.optionalInstant("premium_since");
        this.timeoutUntil = raw.optionalInstant("communication_disabled_until");
        this.pending = raw.bool("pending", false);
        this.flags = MemberFlag.fromBits(raw.optionalInteger("flags").orElse(0));
        this.permissions = raw.optionalString("permissions").map(PermissionSet::fromString);
    }

    @Override
    public DiscordEntityRef<Guild> guild() {
        return this.guild;
    }

    @Override
    public User user() {
        return this.user;
    }

    @Override
    public Optional<String> nickname() {
        return this.nickname;
    }

    @Override
    public Optional<String> avatarHash() {
        return this.avatarHash;
    }

    @Override
    public DiscordEntityRefList<Role> roles() {
        return this.roles;
    }

    @Override
    public Optional<Instant> joinedAt() {
        return this.joinedAt;
    }

    @Override
    public Optional<Instant> boostingSince() {
        return this.boostingSince;
    }

    @Override
    public Optional<Instant> timeoutUntil() {
        return this.timeoutUntil;
    }

    @Override
    public boolean isPending() {
        return this.pending;
    }

    @Override
    public EnumSet<MemberFlag> flags() {
        return EnumSet.copyOf(this.flags);
    }

    @Override
    public Optional<PermissionSet> permissions() {
        return this.permissions;
    }

    @Override
    public String toString() {
        return "Member[" + this.guild.id() + "/" + this.id() + "]";
    }
}
