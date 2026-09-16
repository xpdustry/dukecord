// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.DiscordEntityRefList;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.permission.PermissionSet;
import dev.phinner.dukecord.model.user.User;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

/** A user's membership of a guild. {@link #id()} is the user id; the cache key is {@code (guildId, userId)}. */
public interface Member extends DiscordEntity {

    DiscordEntityRef<Guild> guild();

    User user();

    Optional<String> nickname();

    Optional<String> avatarHash();

    DiscordEntityRefList<Role> roles();

    default boolean hasRole(final Snowflake roleId) {
        return this.roles().contains(roleId);
    }

    Optional<Instant> joinedAt();

    Optional<Instant> boostingSince();

    Optional<Instant> timeoutUntil();

    default boolean isTimedOut() {
        return this.timeoutUntil().filter(until -> until.isAfter(Instant.now())).isPresent();
    }

    boolean isPending();

    EnumSet<MemberFlag> flags();

    /** Discord's precomputed permissions, present only when the member came from an interaction. */
    Optional<PermissionSet> permissions();

    default String displayName() {
        return this.nickname().orElseGet(() -> this.user().displayName());
    }

    default String mention() {
        return this.user().mention();
    }

    /** cache: the highest-positioned cached role, empty if none is cached. */
    default Optional<Role> highestRole() {
        return this.roles().get().stream().max(Comparator.comparingInt(Role::position));
    }

    default void addRole(final Snowflake roleId) {
        this.duke().rest().addMemberRole(this.guild().id(), this.id(), roleId);
    }

    default void addRole(final Snowflake roleId, final String reason) {
        this.duke().rest().addMemberRole(this.guild().id(), this.id(), roleId, reason);
    }

    default void removeRole(final Snowflake roleId) {
        this.duke().rest().removeMemberRole(this.guild().id(), this.id(), roleId);
    }

    default void removeRole(final Snowflake roleId, final String reason) {
        this.duke().rest().removeMemberRole(this.guild().id(), this.id(), roleId, reason);
    }

    default Member edit(final MemberEditSpec spec) {
        return this.duke().rest().editMember(this.guild().id(), this.id(), spec);
    }

    default void kick(final String reason) {
        this.duke().rest().kickMember(this.guild().id(), this.id(), reason);
    }
}
