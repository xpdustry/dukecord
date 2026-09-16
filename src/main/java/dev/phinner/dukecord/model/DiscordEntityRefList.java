// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Function;

/**
 * An ordered set of links to entities Discord only sent the ids of, such as a member's roles. {@link #get()} returns
 * the cached ones in id order and omits misses; {@link #ids()} is always complete.
 */
public interface DiscordEntityRefList<T extends DiscordEntity> {

    Class<T> type();

    SequencedSet<Snowflake> ids();

    boolean contains(final Snowflake id);

    int size();

    boolean isEmpty();

    List<DiscordEntityRef<T>> refs();

    /** The cached snapshots in id order, misses omitted. Never performs I/O. */
    List<T> get();

    /** Fetches every entity over REST, in one request when Discord allows it, one per id otherwise. */
    List<T> fetch();

    static <T extends DiscordEntity> DiscordEntityRefList<T> of(
            final Class<T> type,
            final Collection<Snowflake> ids,
            final Function<Snowflake, Optional<T>> resolver,
            final Function<Snowflake, T> fetcher) {
        return new DiscordEntityRefListImpl<>(type, ids, resolver, fetcher, null);
    }

    static <T extends DiscordEntity> DiscordEntityRefList<T> of(
            final Class<T> type,
            final Collection<Snowflake> ids,
            final Function<Snowflake, Optional<T>> resolver,
            final Function<Snowflake, T> fetcher,
            final Function<Collection<Snowflake>, List<T>> batchFetcher) {
        return new DiscordEntityRefListImpl<>(type, ids, resolver, fetcher, batchFetcher);
    }
}
