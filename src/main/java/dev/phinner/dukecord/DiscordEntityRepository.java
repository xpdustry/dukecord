// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord;

import java.util.List;
import java.util.Optional;

/**
 * Access to Discord objects of one kind, combining the cache with REST. {@code get} methods only read the cache and never block. {@code fetch} methods return the cached entity when present,
 * otherwise request it from Discord and cache the result; {@code force} skips the cache. List fetches always request.
 * Fetches block and throw {@link DiscordRestException} when the request fails.
 *
 * <p>Each repository also exposes {@code rest()}, its kind's REST routes. Every route method maps to one request and
 * never reads or writes the cache. Methods with a {@code reason} parameter record it in the guild audit log.
 */
public interface DiscordEntityRepository<T extends DiscordEntity> {

    interface WithGlobalLookup<T extends DiscordEntity> extends DiscordEntityRepository<T> {

        List<T> getAll();
    }

    interface WithIdLookup<T extends DiscordEntity> extends DiscordEntityRepository<T> {

        Optional<T> getById(final Snowflake id);

        T fetchById(final Snowflake id);

        T fetchById(final Snowflake id, final boolean force);
    }

    interface WithGuildLookup<T extends DiscordEntity> extends DiscordEntityRepository<T> {

        Optional<T> getByGuildAndId(final Snowflake guildId, final Snowflake id);

        T fetchByGuildAndId(final Snowflake guildId, final Snowflake id);

        T fetchByGuildAndId(final Snowflake guildId, final Snowflake id, final boolean force);

        List<T> getAllByGuild(final Snowflake guildId);

        List<T> fetchAllByGuild(final Snowflake guildId);
    }

    interface WithChannelLookup<T extends DiscordEntity> extends DiscordEntityRepository<T> {

        Optional<T> getByChannelAndId(final Snowflake channelId, final Snowflake id);

        T fetchByChannelAndId(final Snowflake channelId, final Snowflake id);

        T fetchByChannelAndId(final Snowflake channelId, final Snowflake id, final boolean force);
    }
}
