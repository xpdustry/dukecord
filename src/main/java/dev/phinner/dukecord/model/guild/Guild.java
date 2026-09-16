// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.guild;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.channel.CategoryChannel;
import dev.phinner.dukecord.model.channel.ChannelCreateSpec;
import dev.phinner.dukecord.model.channel.GuildChannel;
import dev.phinner.dukecord.model.channel.GuildTextChannel;
import dev.phinner.dukecord.model.channel.ThreadChannel;
import dev.phinner.dukecord.model.permission.PermissionCalculator;
import dev.phinner.dukecord.model.permission.PermissionSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A guild snapshot holds guild scalars only. Channels, roles and members live in their own cache stores and are
 * reached through the cache-reading default methods below, which never do I/O.
 */
public interface Guild extends DiscordEntity {

    String name();

    DiscordEntityRef<Member> owner();

    Optional<String> description();

    Optional<String> iconHash();

    Set<String> features();

    /** False during a Discord outage; the data is kept. */
    boolean isAvailable();

    /** Discord's count, from {@code GUILD_CREATE} only. Compare with {@code members().size()}, the known count. */
    Optional<Integer> memberCount();

    Optional<Integer> boostCount();

    MemberCompleteness memberCompleteness();

    // ---- cache navigation ----

    default Optional<Member> member(final Snowflake userId) {
        return this.duke().cache().member(this.id(), userId);
    }

    /** The known members; see {@link #memberCompleteness()}. */
    default List<Member> members() {
        return this.duke().cache().members(this.id());
    }

    /**
     * @throws IllegalStateException if the bot's own member is not cached, which cannot happen after login
     */
    default Member selfMember() {
        return this.member(this.duke().self().id())
                .orElseThrow(() -> new IllegalStateException("Bot member of guild " + this.id() + " is not cached"));
    }

    default Optional<Role> role(final Snowflake roleId) {
        return this.duke().cache().role(roleId).filter(role -> role.guild().id().equals(this.id()));
    }

    /**
     * @throws IllegalStateException if the {@code @everyone} role is not cached, which cannot happen for a cached guild
     */
    default Role everyoneRole() {
        return this.role(this.id())
                .orElseThrow(
                        () -> new IllegalStateException("@everyone role of guild " + this.id() + " is not cached"));
    }

    /** Cached roles, highest position first. */
    default List<Role> roles() {
        return this.duke().cache().roles(this.id()).stream()
                .sorted(Comparator.comparingInt(Role::position).reversed())
                .toList();
    }

    default Optional<GuildChannel> channel(final Snowflake channelId) {
        return this.duke().cache().channel(channelId, GuildChannel.class).filter(c -> c.guild()
                .id()
                .equals(this.id()));
    }

    default <C extends GuildChannel> Optional<C> channel(final Snowflake channelId, final Class<C> type) {
        return this.channel(channelId).filter(type::isInstance).map(type::cast);
    }

    /** Cached channels including threads, by position. */
    default List<GuildChannel> channels() {
        return this.duke().cache().channels(this.id()).stream()
                .sorted(Comparator.comparingInt(GuildChannel::position))
                .toList();
    }

    default List<ThreadChannel> threads() {
        return this.duke().cache().channels(this.id()).stream()
                .filter(ThreadChannel.class::isInstance)
                .map(ThreadChannel.class::cast)
                .toList();
    }

    // ---- computed ----

    /** Guild-level permissions from cached roles. Roles that are not cached are ignored. */
    default PermissionSet permissionsOf(final Member member) {
        return PermissionCalculator.guild(
                this.owner().id(), this.everyoneRole(), member, member.roles().get());
    }

    /**
     * Effective permissions in a channel. Threads resolve through their parent.
     *
     * @throws IllegalStateException if a thread's parent channel is not cached
     */
    default PermissionSet permissionsOf(final Member member, final GuildChannel channel) {
        final var target = channel instanceof ThreadChannel thread
                ? thread.parent()
                        .get()
                        .orElseThrow(
                                () -> new IllegalStateException("Parent of thread " + thread.id() + " is not cached"))
                : channel;
        return PermissionCalculator.channel(this.permissionsOf(member), this.id(), member, target.overwrites());
    }

    // ---- REST ----

    default Member fetchMember(final Snowflake userId) {
        return this.duke().rest().fetchMember(this.id(), userId);
    }

    default List<Member> fetchMembers(final MemberQuery query) {
        return this.duke().rest().fetchMembers(this.id(), query);
    }

    default GuildTextChannel createTextChannel(final ChannelCreateSpec spec) {
        return this.duke().rest().createTextChannel(this.id(), spec);
    }

    default CategoryChannel createCategory(final ChannelCreateSpec spec) {
        return this.duke().rest().createCategory(this.id(), spec);
    }

    default List<ThreadChannel> fetchActiveThreads() {
        return this.duke().rest().fetchActiveThreads(this.id());
    }
}
