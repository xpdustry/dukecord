// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.Snowflake;
import java.util.List;
import java.util.Optional;

/**
 * {@code get} methods only read the cache and never block. {@code fetch} methods return the cached entity when present,
 * otherwise request it from Discord and cache the result; {@code force} skips the cache. List fetches always request.
 * Fetches block and throw {@link java.io.UncheckedIOException} when the request fails.
 */
public interface DiscordEntityStore<T extends DiscordEntity> {

    interface WithGlobalLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        List<T> getAll();

        List<T> fetchAll();
    }

    interface WithIdLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        Optional<T> getById(final Snowflake id);

        T fetchById(final Snowflake id);

        T fetchById(final Snowflake id, final boolean force);
    }

    interface WithGuildLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        Optional<T> getByGuildAndId(final Snowflake guildId, final Snowflake id);

        T fetchByGuildAndId(final Snowflake guildId, final Snowflake id);

        T fetchByGuildAndId(final Snowflake guildId, final Snowflake id, final boolean force);

        List<T> getAllByGuild(final Snowflake guildId);

        List<T> fetchAllByGuild(final Snowflake guildId);
    }

    interface WithChannelLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        Optional<T> getByChannelAndId(final Snowflake channelId, final Snowflake id);

        T fetchByChannelAndId(final Snowflake channelId, final Snowflake id);

        T fetchByChannelAndId(final Snowflake channelId, final Snowflake id, final boolean force);
    }
}
