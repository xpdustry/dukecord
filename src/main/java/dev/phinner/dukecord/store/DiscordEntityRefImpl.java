// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 phinner
package dev.phinner.dukecord.store;

import dev.phinner.dukecord.model.DiscordEntity;
import dev.phinner.dukecord.model.Snowflake;
import java.util.Objects;
import java.util.Optional;

final class DiscordEntityRefImpl<T extends DiscordEntity> implements DiscordEntityRef<T> {

    private final Class<T> type;
    private final Snowflake id;
    private final DiscordEntityStore.WithIdLookup<T> store;

    DiscordEntityRefImpl(final Class<T> type, final Snowflake id, final DiscordEntityStore.WithIdLookup<T> store) {
        this.type = Objects.requireNonNull(type);
        this.id = Objects.requireNonNull(id);
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Class<T> type() {
        return this.type;
    }

    @Override
    public Snowflake id() {
        return this.id;
    }

    @Override
    public Optional<T> get() {
        return this.store.getById(this.id);
    }

    @Override
    public T fetch() {
        return this.store.fetchById(this.id);
    }

    @Override
    public T fetch(final boolean force) {
        return this.store.fetchById(this.id, force);
    }

    @Override
    public boolean equals(final Object other) {
        return this == other
                || (other instanceof DiscordEntityRefImpl<?> that && this.type == that.type && this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.id);
    }

    @Override
    public String toString() {
        return "DiscordEntityRef{" + "type=" + this.type.getSimpleName() + ", id=" + this.id + '}';
    }
}
