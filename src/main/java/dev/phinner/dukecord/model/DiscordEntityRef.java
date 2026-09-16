// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.Optional;
import java.util.function.Function;

/**
 * A link to an entity Discord only sent the id of. {@link #get()} reads the cache and never does I/O;
 * {@link #fetch()} is a blocking REST call. References are equal by target type and id.
 */
public interface DiscordEntityRef<T extends DiscordEntity> {

    Snowflake id();

    Class<T> type();

    /** The cached snapshot, if any. Never performs I/O. */
    Optional<T> get();

    /**
     * Fetches the entity over REST.
     *
     * @throws dev.phinner.dukecord.rest.DiscordNotFoundException if Discord does not know the id
     * @throws dev.phinner.dukecord.rest.DiscordRestException on any other failure
     */
    T fetch();

    static <T extends DiscordEntity> DiscordEntityRef<T> of(
            final Class<T> type,
            final Snowflake id,
            final Function<Snowflake, Optional<T>> resolver,
            final Function<Snowflake, T> fetcher) {
        return new DiscordEntityRefImpl<>(type, id, resolver, fetcher);
    }
}
