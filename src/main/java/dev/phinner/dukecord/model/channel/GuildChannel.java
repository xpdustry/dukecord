// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model.channel;

import dev.phinner.dukecord.model.DiscordEntityRef;
import dev.phinner.dukecord.model.Snowflake;
import dev.phinner.dukecord.model.guild.Guild;
import dev.phinner.dukecord.model.guild.Member;
import dev.phinner.dukecord.model.permission.PermissionOverwrite;
import dev.phinner.dukecord.model.permission.PermissionSet;
import java.util.List;
import java.util.Optional;

public sealed interface GuildChannel extends Channel
        permits CategoryChannel, GuildTextChannel, ThreadChannel, UnknownGuildChannel {

    /** The channel obfuscation flag; when set, every field but id, type, position and parent is fake. */
    int OBFUSCATED_FLAG = 1 << 17;

    DiscordEntityRef<Guild> guild();

    String name();

    int position();

    /** The parent category; always empty for threads, whose parent is a text channel. */
    Optional<DiscordEntityRef<CategoryChannel>> category();

    default boolean isInCategory(final Snowflake categoryId) {
        return this.category().filter(ref -> ref.id().equals(categoryId)).isPresent();
    }

    /** Overwrites may reference roles and members that no longer exist; the ids are not foreign keys. */
    List<PermissionOverwrite> overwrites();

    int flags();

    default boolean isObfuscated() {
        return (this.flags() & OBFUSCATED_FLAG) != 0;
    }

    /**
     * Effective permissions of a member in this channel, computed from the cache.
     *
     * @throws IllegalStateException if the guild is not cached
     */
    default PermissionSet permissionsOf(final Member member) {
        return this.guild()
                .get()
                .orElseThrow(
                        () -> new IllegalStateException("Guild " + this.guild().id() + " is not cached"))
                .permissionsOf(member, this);
    }

    default void delete(final String reason) {
        this.duke().rest().deleteChannel(this.id(), reason);
    }
}
