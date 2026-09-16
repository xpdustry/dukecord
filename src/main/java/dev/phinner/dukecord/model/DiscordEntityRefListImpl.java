// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

final class DiscordEntityRefListImpl<T extends DiscordEntity> implements DiscordEntityRefList<T> {

    private final Class<T> type;
    private final SequencedSet<Snowflake> ids;
    private final Function<Snowflake, Optional<T>> resolver;
    private final Function<Snowflake, T> fetcher;
    private final @Nullable Function<Collection<Snowflake>, List<T>> batchFetcher;

    DiscordEntityRefListImpl(
            final Class<T> type,
            final Collection<Snowflake> ids,
            final Function<Snowflake, Optional<T>> resolver,
            final Function<Snowflake, T> fetcher,
            final @Nullable Function<Collection<Snowflake>, List<T>> batchFetcher) {
        this.type = Objects.requireNonNull(type, "type");
        this.ids = Collections.unmodifiableSequencedSet(new LinkedHashSet<>(Objects.requireNonNull(ids, "ids")));
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
        this.batchFetcher = batchFetcher;
    }

    @Override
    public Class<T> type() {
        return this.type;
    }

    @Override
    public SequencedSet<Snowflake> ids() {
        return this.ids;
    }

    @Override
    public boolean contains(final Snowflake id) {
        return this.ids.contains(id);
    }

    @Override
    public int size() {
        return this.ids.size();
    }

    @Override
    public boolean isEmpty() {
        return this.ids.isEmpty();
    }

    @Override
    public List<DiscordEntityRef<T>> refs() {
        final List<DiscordEntityRef<T>> refs = new ArrayList<>(this.ids.size());
        for (final var id : this.ids) {
            refs.add(new DiscordEntityRefImpl<>(this.type, id, this.resolver, this.fetcher));
        }
        return Collections.unmodifiableList(refs);
    }

    @Override
    public List<T> get() {
        final List<T> result = new ArrayList<>(this.ids.size());
        for (final var id : this.ids) {
            this.resolver.apply(id).ifPresent(result::add);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<T> fetch() {
        if (this.batchFetcher != null) {
            return List.copyOf(this.batchFetcher.apply(this.ids));
        }
        final List<T> result = new ArrayList<>(this.ids.size());
        for (final var id : this.ids) {
            result.add(this.fetcher.apply(id));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return other instanceof DiscordEntityRefListImpl<?> list && list.type == this.type && list.ids.equals(this.ids);
    }

    @Override
    public int hashCode() {
        return 31 * this.type.hashCode() + this.ids.hashCode();
    }

    @Override
    public String toString() {
        return "Refs<" + this.type.getSimpleName() + ">" + this.ids;
    }
}
