// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

final class DiscordEntityRefImpl<T extends DiscordEntity> implements DiscordEntityRef<T> {

    private final Class<T> type;
    private final Snowflake id;
    private final Function<Snowflake, Optional<T>> resolver;
    private final Function<Snowflake, T> fetcher;

    DiscordEntityRefImpl(
            final Class<T> type,
            final Snowflake id,
            final Function<Snowflake, Optional<T>> resolver,
            final Function<Snowflake, T> fetcher) {
        this.type = Objects.requireNonNull(type, "type");
        this.id = Objects.requireNonNull(id, "id");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
    }

    @Override
    public Snowflake id() {
        return this.id;
    }

    @Override
    public Class<T> type() {
        return this.type;
    }

    @Override
    public Optional<T> get() {
        return this.resolver.apply(this.id);
    }

    @Override
    public T fetch() {
        return this.fetcher.apply(this.id);
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return other instanceof DiscordEntityRefImpl<?> ref && ref.type == this.type && ref.id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return 31 * this.type.hashCode() + this.id.hashCode();
    }

    @Override
    public String toString() {
        return "Ref<" + this.type.getSimpleName() + ">[" + this.id + "]";
    }
}
