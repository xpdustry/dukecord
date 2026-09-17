// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.Snowflake;
import java.util.List;
import java.util.Optional;

public interface DiscordEntityStore<T extends DiscordEntity> {

    interface WithGlobalLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        List<T> getAll();

        List<T> getAll(final LookupStrategy strategy);
    }

    interface WithIdLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        Optional<T> getById(final Snowflake id);

        Optional<T> getById(final Snowflake id, final LookupStrategy strategy);
    }

    interface WithGuildLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        Optional<T> getByGuildAndId(final Snowflake guildId, final Snowflake id);

        Optional<T> getByGuildAndId(final Snowflake guildId, final Snowflake id, final LookupStrategy strategy);

        List<T> getAllByGuild(final Snowflake guildId);

        List<T> getAllByGuild(final Snowflake guildId, final LookupStrategy strategy);
    }

    interface WithChannelLookup<T extends DiscordEntity> extends DiscordEntityStore<T> {

        Optional<T> getByChannelAndId(final Snowflake channelId, final Snowflake id);

        Optional<T> getByChannelAndId(final Snowflake channelId, final Snowflake id, final LookupStrategy strategy);
    }
}
